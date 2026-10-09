# Copyright 2026 OLO Labs
# SPDX-License-Identifier: Apache-2.0
"""Run the actual POSIX projected-secret tests as the production numeric UID on Linux."""
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import tempfile

ROOT = Path(__file__).resolve().parents[2]
JDK = 'eclipse-temurin:21-jdk-noble@sha256:70898f0f893a6b772a0f29834d8b022e3ac20b6a0c33a922973cf66342ef56be'


def main():
    version = re.search(r'junit = "([^"]+)"', (ROOT/'gradle/libs.versions.toml').read_text()).group(1)
    platform = '1.'+version.split('.', 1)[1]
    cache = Path(os.environ.get('GRADLE_USER_HOME', Path.home()/'.gradle'))/'caches/modules-2/files-2.1'
    dependencies = [('org.junit.jupiter', 'junit-jupiter-api', version),
                    ('org.junit.platform', 'junit-platform-commons', platform),
                    ('org.opentest4j', 'opentest4j', '1.3.0'),
                    ('org.apiguardian', 'apiguardian-api', '1.1.2')]
    with tempfile.TemporaryDirectory(prefix='toolgate-linux-custody-') as temp:
        folder = Path(temp)
        for group, artifact, release in dependencies:
            jars = list((cache/group/artifact/release).glob('*/'+artifact+'-'+release+'.jar'))
            if len(jars) != 1:
                raise RuntimeError('Run the Gradle test gate first to resolve '+artifact)
            shutil.copyfile(jars[0], folder/jars[0].name)
        base = ROOT/'apps/control-plane/src'
        for source in (base/'main/java/io/ololabs/toolgate/control/adapter/ProtectedCustody.java',
                       base/'test/java/io/ololabs/toolgate/control/adapter/ProtectedCustodyTest.java'):
            shutil.copyfile(source, folder/source.name)
        (folder/'CustodyProof.java').write_text('''package io.ololabs.toolgate.control.adapter;
import java.nio.file.*;
public class CustodyProof {
    public static void main(String[] args) throws Exception {
        var test = new ProtectedCustodyTest();
        test.temp = Files.createTempDirectory(Path.of(System.getProperty("user.home")), "proof-");
        test.custodyRejectsRelativePathsAndNonRegularMaterial();
        test.projectedKeysStayWithinMountAndCannotBeWorldReadableOrWritable();
        System.out.println("PASS Linux projected custody, escapes, size and private permissions; no skips");
    }
}
''', encoding='utf-8')
        (folder/'Dockerfile').write_text('FROM '+JDK+'''
WORKDIR /opt/proof
COPY . .
RUN mkdir classes && javac -cp '*' -d classes *.java && mkdir -p /home/custody
USER 65532:65532
ENTRYPOINT ["java","-Duser.home=/home/custody","-cp","/opt/proof/classes:/opt/proof/*","io.ololabs.toolgate.control.adapter.CustodyProof"]
''', encoding='utf-8')
        image = 'olo-toolgate-custody:verification'
        subprocess.run(['docker', 'build', '-t', image, str(folder)], check=True)
        subprocess.run(['docker', 'run', '--rm', '--read-only', '--cap-drop=ALL',
                        '--security-opt=no-new-privileges', '--memory=256m',
                        '--tmpfs', '/home/custody:rw,noexec,nosuid,mode=0700,uid=65532,gid=65532', image], check=True)
    output = ROOT/'build/enterprise'; output.mkdir(parents=True, exist_ok=True)
    (output/'linux-custody.json').write_text(json.dumps(dict(platform='Linux', user='65532:65532',
        readOnly=True, tests=['custodyRejectsRelativePathsAndNonRegularMaterial',
        'projectedKeysStayWithinMountAndCannotBeWorldReadableOrWritable'], skipped=0), indent=2)+'\n', encoding='utf-8')


if __name__ == '__main__': main()
