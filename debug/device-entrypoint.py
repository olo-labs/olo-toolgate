# Copyright 2026 OLO Labs
# SPDX-License-Identifier: Apache-2.0
"""Install and supervise a real Linux client, with loopback forwarding to debug Quickstart."""
import json
import os
from pathlib import Path
import re
import select
import shutil
import signal
import socket
import socketserver
import subprocess
import threading
import time
import urllib.request

CLIENT = '/usr/local/lib/olo-toolgate/olo-toolgate-client'
JOURNAL = Path('/var/lib/olo-toolgate/journal.json')
STATUS = Path('/run/olo-toolgate/device-status.json')
STOP = threading.Event()


class Relay(socketserver.ThreadingTCPServer):
    allow_reuse_address = True
    daemon_threads = True

    def __init__(self, local_port, upstream, upstream_port):
        self.upstream = (upstream, upstream_port)
        super().__init__(('127.0.0.1', local_port), Forward)


class Forward(socketserver.BaseRequestHandler):
    def handle(self):
        try:
            with socket.create_connection(self.server.upstream, timeout=10) as remote:
                remote.settimeout(None)
                # Forward bytes unchanged, including TLS and WebSockets. The client validates TLS.
                peers = {self.request: remote, remote: self.request}
                readers = list(peers)
                while readers and not STOP.is_set():
                    readable, _, _ = select.select(readers, [], [], 1)
                    for source in readable:
                        data = source.recv(65536)
                        if data:
                            peers[source].sendall(data)
                        else:
                            peers[source].shutdown(socket.SHUT_WR)
                            readers.remove(source)
        except OSError:
            # A closed upstream connection must reach the client's ordinary retry handling.
            return


def cli(operation):
    result = subprocess.run([CLIENT, operation], capture_output=True, text=True, timeout=25)
    if result.returncode:
        raise RuntimeError(f'Client {operation} failed: {result.stderr.strip()}')
    return result.stdout


def enrollment():
    output = cli('enroll')
    fingerprint = re.search(r'fingerprint in your browser: ([a-f0-9]{64})', output)
    code = re.search(r'[?&]code=([A-F0-9]{16})', output)
    expires = re.search(r'Enrollment expires at (\d+)', output)
    if not (fingerprint and code and expires):
        raise RuntimeError('Client did not return an enrollment code and fingerprint.')
    prompt = {'deviceId': 'device-' + fingerprint[1][:32], 'keyFingerprint': fingerprint[1],
              'userCode': code[1], 'expiresAtUnixMs': int(expires[1])}
    print(f'Needs approval: {prompt["deviceId"]}; code={prompt["userCode"]}; '
          f'fingerprint={prompt["keyFingerprint"]}', flush=True)
    return prompt


def publish(health, prompt):
    status = {'platform': 'LINUX', 'systemName': socket.gethostname(), **health}
    if prompt:
        status.update(prompt)
    if JOURNAL.exists():
        journal = json.loads(JOURNAL.read_text())
        identity = journal.get('identity')
        if identity:
            status['deviceId'] = identity['deviceId']
            # Enrollment codes are no longer useful once the identity has been issued.
            status.pop('userCode', None)
            status.pop('expiresAtUnixMs', None)
    temporary = STATUS.with_suffix('.tmp')
    temporary.write_text(json.dumps(status))
    temporary.replace(STATUS)


def main():
    signal.signal(signal.SIGTERM, lambda *_: STOP.set())
    signal.signal(signal.SIGINT, lambda *_: STOP.set())
    upstream = os.environ['TOOLGATE_DEVICE_UPSTREAM']
    relays = [Relay(18090, upstream, 8080), Relay(18450, upstream, 8443)]
    for relay in relays:
        threading.Thread(target=relay.serve_forever, daemon=True).start()
    service = None
    try:
        deadline = time.monotonic() + 90
        while not STOP.is_set():
            try:
                with urllib.request.urlopen('http://127.0.0.1:18090/health/ready', timeout=3) as response:
                    if response.status == 200:
                        break
            except OSError:
                pass
            if time.monotonic() >= deadline:
                raise RuntimeError('Debug Quickstart is not ready or reachable on its Docker network.')
            STOP.wait(1)
        if STOP.is_set():
            return
        config = Path('/etc/olo-toolgate/client.json')
        if not config.exists():
            print('Installing client and verifying gateway certificate trust.', flush=True)
            subprocess.run(['/opt/toolgate/client', 'install', '--server', 'https://localhost:18450'],
                           check=True, timeout=60)
        else:
            settings = json.loads(config.read_text())
            if settings['serverUrl'] != 'https://localhost:18450':
                raise RuntimeError('Existing device belongs to a different gateway.')
            # A recreated container gets the current executable and retains its protected identity.
            shutil.copyfile('/opt/toolgate/client', CLIENT)
            os.chmod(CLIENT, 0o755)
        STATUS.parent.mkdir(mode=0o755, parents=True, exist_ok=True)
        service = subprocess.Popen([CLIENT, 'service'])
        deadline = time.monotonic() + 60
        while not STOP.is_set():
            if service.poll() is not None:
                raise RuntimeError('The protected client service exited during startup.')
            try:
                health = json.loads(cli('health'))
                break
            except (RuntimeError, subprocess.TimeoutExpired, ValueError):
                if time.monotonic() >= deadline:
                    raise RuntimeError('The protected client IPC did not become ready.')
                STOP.wait(1)
        if STOP.is_set():
            return
        prompt = enrollment() if health['state'] in ('UNENROLLED', 'PENDING') else None
        previous = health['state']
        while not STOP.is_set():
            if service.poll() is not None:
                raise RuntimeError('The protected client service exited.')
            health = json.loads(cli('health'))
            # Refresh an expired pending request. A deliberate denial stops automatic enrollment.
            if previous == 'PENDING' and health['state'] == 'UNENROLLED' and prompt:
                prompt = enrollment() if time.time() * 1000 >= prompt['expiresAtUnixMs'] else None
                if prompt:
                    health = json.loads(cli('health'))
            publish(health, prompt)
            previous = health['state']
            STOP.wait(5)
    finally:
        if service and service.poll() is None:
            service.terminate()
            try:
                service.wait(timeout=20)
            except subprocess.TimeoutExpired:
                service.kill()
                service.wait()
        for relay in relays:
            relay.shutdown()
            relay.server_close()


if __name__ == '__main__':
    try:
        main()
    except Exception as failure:
        print(f'Device startup failed: {failure}', flush=True)
        raise SystemExit(1)
