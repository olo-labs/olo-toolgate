# Copyright 2026 OLO Labs; SPDX-License-Identifier: Apache-2.0
"""External OCI custody exports public bootstrap data and never enables native fallback."""
import datetime
import importlib.util
import json
import os
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch
from cryptography import x509
from cryptography.hazmat.primitives import hashes, serialization
from cryptography.hazmat.primitives.asymmetric import rsa
from cryptography.x509.oid import NameOID

ROOT = Path(__file__).resolve().parents[2]
spec = importlib.util.spec_from_file_location('external_host', ROOT/'apps/quickstart/supervisor.py')
quickstart = importlib.util.module_from_spec(spec)
spec.loader.exec_module(quickstart)


class ExternalHostTests(unittest.TestCase):
    def environment(self):
        return dict(TOOLGATE_QUICKSTART_EXTERNAL_TOOL_HOST='true', **{
            name: 'registry.example/toolgate/image@sha256:'+char*64
            for name, char in (('TOOLGATE_TOOL_HOST_PYTHON_IMAGE','a'), ('TOOLGATE_TOOL_HOST_JVM_IMAGE','b'), ('TOOLGATE_TOOL_HOST_BROKER_IMAGE','c'))})

    def test_native_default_is_unchanged_and_external_mode_needs_every_exact_pin(self):
        with patch.dict(os.environ, {}, clear=True):
            self.assertIsNone(quickstart.external_tool_host_sandbox())
        with patch.dict(os.environ, self.environment(), clear=True):
            self.assertEqual('/usr/bin/podman', quickstart.external_tool_host_sandbox()['podmanPath'])
        for name in self.environment():
            env = self.environment(); env[name] = 'latest'
            with patch.dict(os.environ, env, clear=True), self.assertRaises(ValueError):
                quickstart.external_tool_host_sandbox()

    def test_bootstrap_contains_only_public_material_and_preserves_the_private_config(self):
        key = rsa.generate_private_key(public_exponent=65537, key_size=2048)
        subject = x509.Name([x509.NameAttribute(NameOID.COMMON_NAME,'test')])
        now = datetime.datetime.now(datetime.timezone.utc)
        cert = x509.CertificateBuilder().subject_name(subject).issuer_name(subject).public_key(key.public_key()).serial_number(1).not_valid_before(now).not_valid_after(now+datetime.timedelta(days=1)).sign(key, hashes.SHA256())
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            public = root/'public.pem'; public.write_bytes(key.public_key().public_bytes(serialization.Encoding.PEM, serialization.PublicFormat.SubjectPublicKeyInfo))
            ca = root/'ca.pem'; ca.write_bytes(cert.public_bytes(serialization.Encoding.PEM))
            (root/'private.key').write_bytes(key.private_bytes(serialization.Encoding.PEM, serialization.PrivateFormat.PKCS8, serialization.NoEncryption()))
            config = dict(hostId='tool-host-local', permitKeys=[dict(kid='k',publicKeyPath=str(public))], control=dict(tenantId='tenant',url='https://localhost:18453',caCertificatePath=str(ca)), storeDir='/data/store', workDir='/data/work', enrollment=dict(identityDir='/data/state/tool-host'), sandbox={})
            before = json.dumps(config)
            with patch.object(quickstart,'DATA',root):
                quickstart.export_tool_host_bootstrap(config)
                first = {p.name:p.read_bytes() for p in (root/'tool-host-bootstrap').iterdir()}
                quickstart.export_tool_host_bootstrap(config)
                self.assertEqual(first, {p.name:p.read_bytes() for p in (root/'tool-host-bootstrap').iterdir()})
            self.assertEqual(before,json.dumps(config))
            self.assertEqual({'tool-host.json','permit-key-0.pem','device-ca.crt'},set(first))
            self.assertNotIn(b'PRIVATE KEY',b''.join(first.values()))
            exported = json.loads(first['tool-host.json'])
            self.assertEqual('https://localhost:18453',exported['control']['url'])
            self.assertEqual('/etc/toolgate/permit-key-0.pem',exported['permitKeys'][0]['publicKeyPath'])
            self.assertEqual('/var/lib/toolgate-tool-host/identity',exported['enrollment']['identityDir'])
            (root/'tool-host-bootstrap/foreign.key').write_bytes(b'preserve private state')
            before = {p.name:p.read_bytes() for p in (root/'tool-host-bootstrap').iterdir()}
            with patch.object(quickstart,'DATA',root), self.assertRaises(ValueError):
                quickstart.export_tool_host_bootstrap(config)
            self.assertEqual(before,{p.name:p.read_bytes() for p in (root/'tool-host-bootstrap').iterdir()})
