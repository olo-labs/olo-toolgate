# Copyright 2026 OLO Labs
# SPDX-License-Identifier: Apache-2.0
import hashlib
import json
import tempfile
import unittest
import zipfile
from pathlib import Path
from extension import package, ROOT

class ExtensionTest(unittest.TestCase):
    def test_deterministic_licensed_versioned_package(self):
        with tempfile.TemporaryDirectory() as work:
            output=Path(work)
            metadata=package(output,23)
            first=(output/metadata['filename']).read_bytes()
            package(output,23)
            self.assertEqual(first,(output/metadata['filename']).read_bytes())
            self.assertEqual(hashlib.sha256(first).hexdigest(),metadata['sha256'])
            with zipfile.ZipFile(output/metadata['filename']) as archive:
                self.assertTrue({'LICENSE','NOTICE.md','RELEASE-NOTES.md','README.md','manifest.json','worker.js','popup.js','bridge.js'}<=set(archive.namelist()))
                manifest=json.loads(archive.read('manifest.json'))
                self.assertEqual(manifest['version_name'],metadata['version'])
                self.assertEqual(manifest['version'],metadata['chromeVersion'])
                self.assertEqual(manifest['manifest_version'],3)
            identity=json.loads((ROOT/'apps/browser-extension/identity.json').read_text())
            import base64
            digest=hashlib.sha256(base64.b64decode(identity['key'])).digest()[:16]
            identifier=''.join(chr(97+(b>>4))+chr(97+(b&15)) for b in digest)
            self.assertEqual(identifier,metadata['extensionId'])
            self.assertIn(identifier,(ROOT/'apps/endpoint-client/src/bin/olo-toolgate-browser-host.rs').read_text())
            self.assertIn(identifier,(ROOT/'apps/endpoint-client/packaging/windows-setup.iss').read_text())
            for path in ['apps/admin-ui/src/Connect.tsx','apps/control-plane/src/main/java/io/ololabs/toolgate/control/adapter/ClientArtifacts.java','tools/client/installer_smoke.py']:
                self.assertIn(identifier,(ROOT/path).read_text(encoding='utf-8'))

    def test_chrome_version_bounds(self):
        with tempfile.TemporaryDirectory() as work:
            for invalid in [-1,65536]:
                with self.assertRaises(ValueError):package(Path(work),invalid)
