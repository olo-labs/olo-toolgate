# Copyright 2026 OLO Labs
# SPDX-License-Identifier: Apache-2.0
"""Real OCI adapter protocol/security gate; owns only nonce-named fixtures and test state."""
import argparse
import json
import os
import re
import shutil
import subprocess
import tempfile
import urllib.request
import hashlib
import uuid
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
PYTHON = 'python@sha256:ce40764625a4ff50df3548277632e7f96c4e77fe75fa848aae9885476e7df5a4'
NODE = 'node:22-bookworm-slim@sha256:43ac6c60b8f89723f746e8a92ce91abd5017e627ce1ddfe4238355d3a30b772c'
JDK = 'eclipse-temurin:21-jdk-noble@sha256:70898f0f893a6b772a0f29834d8b022e3ac20b6a0c33a922973cf66342ef56be'
JRE = 'eclipse-temurin:21-jre-noble@sha256:22138efd69393501fccd8176ae16b01791ed71ff801b28f0359415389b17c766'
POWERSHELL = 'mcr.microsoft.com/powershell@sha256:7ab5bd5ca6f95a3351fa0c6a1205237d57048c94542355aab55519a0861a9b25'
DOTNET_SDK = 'mcr.microsoft.com/dotnet/sdk@sha256:e70cdb7f80b0348f5cb85f19a8f670fca061f033d57eed12fa003d58b0e06317'
DOTNET = 'mcr.microsoft.com/dotnet/runtime@sha256:b89586dc17781f25531909993658aa8161205ae38b8cec8847df4a8221a403d5'
PULL_IMAGE = 'python@sha256:bb2988715db2cf7ace7b53f38f3cffbef7c7046a656bee66245eb0ed386e2e81'


def run(args, **kwargs):
    return subprocess.run(args, cwd=ROOT, check=True, **kwargs)


def pinned_python_version(image):
    """Read version from digest-verified primary registry metadata, not a mutable tag."""
    endpoint='https://registry-1.docker.io/v2/library/python/'
    token=json.load(urllib.request.urlopen('https://auth.docker.io/token?service=registry.docker.io&scope=repository:library/python:pull',timeout=15))['token']
    def fetch(path,digest):
        request=urllib.request.Request(endpoint+path,headers={'Authorization':'Bearer '+token,'Accept':'application/vnd.oci.image.index.v1+json, application/vnd.oci.image.manifest.v1+json'})
        raw=urllib.request.urlopen(request,timeout=15).read(1048577)
        if len(raw)>1048576 or 'sha256:'+hashlib.sha256(raw).hexdigest()!=digest:raise ValueError('Pinned registry metadata digest mismatch')
        return json.loads(raw)
    digest=image.split('@')[1]
    index=fetch('manifests/'+digest,digest)
    architecture=run(['docker','info','--format','{{.Architecture}}'],capture_output=True,text=True).stdout.strip()
    architecture={'x86_64':'amd64','aarch64':'arm64'}.get(architecture,architecture)
    manifest=next(m for m in index['manifests'] if m['platform']['os']=='linux' and m['platform']['architecture']==architecture)
    manifest=fetch('manifests/'+manifest['digest'],manifest['digest'])
    config=fetch('blobs/'+manifest['config']['digest'],manifest['config']['digest'])
    return next(v.split('=',1)[1] for v in config['config']['Env'] if v.startswith('PYTHON_VERSION='))


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--test-binary', type=Path, help='Already compiled native Rust execution test executable')
    parser.add_argument('--native-fixture', type=Path, required=True, help='Real Linux x64/ARM64 runtime_fixture executable')
    args = parser.parse_args()
    (ROOT/'.dev').mkdir(exist_ok=True)
    engine = Path(shutil.which('docker') or '')
    if not engine.is_file():
        raise SystemExit('A local Docker engine is required for this explicit gate')
    base = Path(os.environ['ProgramData']) if os.name == 'nt' else Path(tempfile.gettempdir())
    state = base / ('toolgate-runtime-test-' + uuid.uuid4().hex)
    if os.name != 'nt':
        state.mkdir(mode=0o700)
    endpoint = 'npipe:////./pipe/dockerDesktopLinuxEngine' if os.name == 'nt' else 'unix:///var/run/docker.sock'
    tags = []
    try:
        with tempfile.TemporaryDirectory(prefix='runtime-images-', dir=ROOT/'.dev') as temporary:
            work = Path(temporary)
            for source in (ROOT/'tools/client/runtime-fixtures').iterdir():
                shutil.copyfile(source, work/source.name)
            shutil.copyfile(args.native_fixture, work/'run')
            images = {}
            plans = {
                'PYTHON': (f'FROM {PYTHON}\nENV IMAGE_SECRET_SENTINEL=should-not-leak\nCOPY --chmod=0444 python.py /opt/tool/tool.py\n', ['/usr/local/bin/python3', '--version']),
                'NODE': (f'FROM {NODE}\nCOPY --chmod=0444 node.mjs /opt/tool/tool.mjs\n', ['/usr/local/bin/node', '--version']),
                'SHELL': (f'FROM {PYTHON}\nCOPY --chmod=0444 shell.sh /opt/tool/tool.sh\n', ['/bin/bash', '--version']),
                'NATIVE': (f'FROM {PYTHON}\nCOPY --chmod=0555 run /opt/tool/run\n', ['/opt/tool/run', '--version']),
                'JAVA_JAR': (f'FROM {JDK} AS compile\nWORKDIR /source\nCOPY Tool.java .\nRUN javac Tool.java && jar --create --file tool.jar --main-class Tool Tool.class\nFROM {JRE}\nCOPY --from=compile --chmod=0444 /source/tool.jar /opt/tool/tool.jar\n', ['/opt/java/openjdk/bin/java', '-version']),
                'POWERSHELL': (f'FROM {POWERSHELL}\nCOPY --chmod=0444 tool.ps1 /opt/tool/tool.ps1\n', ['/usr/bin/pwsh', '--version']),
                'DOTNET': (f'FROM {DOTNET_SDK} AS compile\nWORKDIR /source\nCOPY Program.cs tool.csproj ./\nRUN dotnet publish tool.csproj --configuration Release --output /output\nFROM {DOTNET}\nCOPY --from=compile /output/ /opt/tool/\n', ['/usr/share/dotnet/dotnet', '--list-runtimes']),
            }
            for kind, (plan, version_command) in plans.items():
                tag = 'olo-toolgate-runtime-test:' + uuid.uuid4().hex
                tags.append(tag)
                (work/'Dockerfile').write_text('# Copyright 2026 OLO Labs\n# SPDX-License-Identifier: Apache-2.0\n'+plan+'RUN chmod 0755 /opt /opt/tool\n', encoding='utf-8')
                run(['docker', 'build', '-t', tag, str(work)])
                identity = run(['docker', 'image', 'inspect', '--format', '{{.Id}}', tag], capture_output=True, text=True).stdout.strip()
                probe = run(['docker', 'run', '--rm', '--network', 'none', '--entrypoint', version_command[0], identity, *version_command[1:]], capture_output=True, text=True)
                first = (probe.stdout + probe.stderr).splitlines()[0]
                version = re.search(r'(?<!\d)(\d+\.\d+\.\d+(?:\.\d+)?)', first)
                if not version:
                    raise ValueError('Version probe did not produce exact SemVer')
                exact_version = version[1]
                if kind == 'JAVA_JAR' and exact_version.count('.') == 3:
                    exact_version = exact_version.rsplit('.', 1)[0] + '+' + exact_version.rsplit('.', 1)[1]
                images[kind] = {'image': identity, 'version': exact_version}
            # A separate pinned official Python image exercises actual provisioning.
            # Previously cached images are reused; user images are never deleted.
            pull_version = pinned_python_version(PULL_IMAGE)
            initially_present = subprocess.run(['docker','image','inspect',PULL_IMAGE],capture_output=True).returncode == 0
            env = dict(os.environ, TOOLGATE_RUNTIME_TEST_IMAGES=json.dumps(images),
                       TOOLGATE_RUNTIME_TEST_STATE=str(state), TOOLGATE_RUNTIME_TEST_ENGINE=str(engine),
                       TOOLGATE_RUNTIME_TEST_ENDPOINT=endpoint, TOOLGATE_RUNTIME_LEAK_SENTINEL='should-not-leak',
                       TOOLGATE_RUNTIME_PULL_IMAGE=PULL_IMAGE, TOOLGATE_RUNTIME_PULL_VERSION=pull_version)
            command = [str(args.test_binary.resolve())] if args.test_binary else ['cargo', 'test', '-p', 'olo-toolgate-client', '--test', 'execution', '--locked', '--']
            run([*command, 'real_managed_runtime_security_boundary', '--ignored', '--nocapture'], env=env)
            evidence = ROOT/'build/client/runtime-smoke.json'
            evidence.parent.mkdir(parents=True, exist_ok=True)
            evidence.write_text(json.dumps({'realDocker': True, 'adapters': images, 'jsonProtocol': True,
                'argumentInjection': True, 'childProcessesDenied': True, 'networkDenied': True,
                'environmentCleared': True, 'timeoutOutputMemory': True, 'malformedAndWrongVersionRejected': True,
                'onlineAuthorizationRequired': True, 'firstUseProvisionOrCache': True, 'initiallyPresentRuntimeImage': initially_present, 'cleanup': True}, indent=2)+'\n', encoding='utf-8')
            print('Real managed runtime adapter/security gate passed')
    finally:
        for tag in tags:
            subprocess.run(['docker', 'image', 'rm', tag], cwd=ROOT, capture_output=True)
        # Single-interpreter deletion of this verified nonce-named test directory only.
        if state.resolve().parent != base.resolve() or not state.name.startswith('toolgate-runtime-test-'):
            raise ValueError('Owned test path verification failed')
        if state.exists():
            shutil.rmtree(state)


if __name__ == '__main__':
    main()
