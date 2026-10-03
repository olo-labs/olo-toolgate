# Copyright 2026 OLO Labs
# SPDX-License-Identifier: Apache-2.0
"""Real malicious/normal protocol fixture; never shipped as a production tool."""
import json
import os
import socket
import subprocess
import sys
import threading
import time

request = json.loads(sys.stdin.buffer.read(65537))
args = request['arguments']
mode = args['mode']
output = {'text': args['text']}
if mode == 'timeout':
    time.sleep(60)
elif mode == 'overflow':
    sys.stdout.write('x' * 100000)
    sys.exit(0)
elif mode == 'memory':
    hold = bytearray(1024 * 1024 * 1024)
elif mode == 'malformed':
    print('not json')
    sys.exit(0)
elif mode == 'duplicate':
    print('{"protocolVersion":1,"requestId":"runtime-request","requestId":"other","output":{}}')
    sys.exit(0)
elif mode == 'wrong-request':
    request['requestId'] = 'other'
elif mode == 'stderr':
    sys.stderr.write('secret-redaction-sentinel')
elif mode == 'wrong-schema':
    output['undeclared'] = True
elif mode == 'isolation':
    failures = []
    for operation in [lambda: subprocess.run(['/bin/true'], check=True),
                      lambda: socket.socket(socket.AF_INET, socket.SOCK_STREAM),
                      lambda: open('/escape', 'w'),
                      lambda: open('/var/run/docker.sock', 'r'),
                      lambda: open('/var/lib/olo-toolgate/device-key', 'r')]:
        try:
            operation()
            failures.append(True)
        except OSError:
            pass
    seen = []
    thread = threading.Thread(target=lambda: seen.append(True))
    thread.start()
    thread.join()
    output['threads'] = bool(seen)
    output['isolated'] = not failures and not any(k in os.environ for k in
        ['TOOLGATE_RUNTIME_LEAK_SENTINEL', 'IMAGE_SECRET_SENTINEL', 'HTTP_PROXY', 'PYTHONPATH', 'NODE_OPTIONS']) and os.getuid() == 65532 and os.getcwd() == '/work'
print(json.dumps({'protocolVersion': 1, 'requestId': request['requestId'], 'output': output}))
