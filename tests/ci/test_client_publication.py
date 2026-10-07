# Copyright 2026 OLO Labs
# SPDX-License-Identifier: Apache-2.0
from pathlib import Path
import sys
import unittest

sys.path.insert(0, str(Path(__file__).resolve().parents[2] / 'tools/client'))
from publication import public_asset


class ClientPublicationTest(unittest.TestCase):
    def test_windows_build_archives_never_become_public_assets(self):
        for target in ('x86_64-pc-windows-msvc', 'aarch64-pc-windows-msvc'):
            base = f'olo-toolgate-client-0.10.0-dev-{target}'
            for suffix in ('.zip', '.zip.sha256'):
                self.assertFalse(public_asset(Path(base + suffix)))
            for suffix in ('.setup.exe', '.setup.exe.sha256'):
                self.assertTrue(public_asset(Path(base + suffix)))

    def test_extension_zip_is_internal_and_installers_are_preserved(self):
        self.assertFalse(public_asset(Path('olo-toolgate-chrome-0.10.0-dev-0.zip')))
        self.assertFalse(public_asset(Path('connect-installers.json')))
        self.assertFalse(public_asset(Path('olo-toolgate-connect-0.10.0-dev-x86_64-pc-windows-msvc.setup.exe')))
        for name in ('installers.json',
                     'olo-toolgate-client-x86_64-apple-darwin.dmg',
                     'olo-toolgate-client-x86_64-unknown-linux-gnu.run'):
            self.assertTrue(public_asset(Path(name)))
