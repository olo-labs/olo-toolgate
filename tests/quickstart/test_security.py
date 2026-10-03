# Copyright 2026 OLO Labs
# SPDX-License-Identifier: Apache-2.0
"""Real local identity primitives and offline custody/integrity tests; run in Linux image."""
import importlib.util
import json
import os
from pathlib import Path
import sqlite3
import tempfile
import unittest
from unittest.mock import patch

ROOT=Path(__file__).resolve().parents[2]
spec=importlib.util.spec_from_file_location('quickstart_supervisor',ROOT/'apps/quickstart/supervisor.py')
quickstart=importlib.util.module_from_spec(spec);spec.loader.exec_module(quickstart)


class SecurityTests(unittest.TestCase):
    def setUp(self):
        self.temp=tempfile.TemporaryDirectory();self.root=Path(self.temp.name);self.identity=quickstart.LocalIdentity()
        quickstart.DATA=self.root/'data';quickstart.DATA.mkdir(mode=0o700)
        (quickstart.DATA/'keys').mkdir(mode=0o700)
        (quickstart.DATA/'state').mkdir(mode=0o700)
        with sqlite3.connect(quickstart.DATA/'state/control.sqlite') as db:
            for version in (1,2):db.executescript((ROOT/f'apps/control-plane/src/main/resources/db/quickstart/V{version}.sql').read_text())
    def tearDown(self):self.temp.cleanup()
    def test_password_policy_rotation_signature_and_stale_generation(self):
        for password in ('','short','a'*32,'abc def ghi jkl mno','é'*32,123):
            self.assertFalse(quickstart.password_valid(password))
        bootstrap='initial-Strong-Password-2026'
        quickstart.atomic(quickstart.DATA/'identity.json',json.dumps(quickstart.password_record(bootstrap)))
        with self.assertRaises(PermissionError):self.identity.authenticate('wrong-password')
        with self.assertRaises(ValueError):self.identity.authenticate(bootstrap)
        token=self.identity.authenticate(bootstrap,'replacement-Strong-Password-2026')
        self.assertEqual(quickstart.session('Bearer '+token)['sub'],'admin')
        with self.assertRaises(PermissionError):quickstart.session('Bearer '+token[:-4]+'xxxx')
        with self.assertRaises(PermissionError):quickstart.session('Bearer '+quickstart.jwt(['toolgate-admin']))
        self.identity.authenticate('replacement-Strong-Password-2026','another-Strong-Password-2026')
        with self.assertRaises(PermissionError):quickstart.session('Bearer '+token)
        with sqlite3.connect(quickstart.DATA/'state/control.sqlite') as db:
            events=db.execute("SELECT operation,revision FROM control_audit WHERE target='local-identity' ORDER BY sequence").fetchall()
            self.assertEqual(events,[('PASSWORD_CHANGE_REQUESTED',2),('PASSWORD_CHANGED',2),('PASSWORD_CHANGE_REQUESTED',3),('PASSWORD_CHANGED',3)])
            db.execute("CREATE TRIGGER reject_change BEFORE INSERT ON control_audit WHEN NEW.operation='PASSWORD_CHANGE_REQUESTED' BEGIN SELECT RAISE(ABORT,'audit unavailable'); END")
        before=(quickstart.DATA/'identity.json').read_bytes()
        with self.assertRaises(sqlite3.IntegrityError):self.identity.authenticate('another-Strong-Password-2026','final-Strong-Password-2026')
        self.assertEqual(before,(quickstart.DATA/'identity.json').read_bytes())
    def test_weak_first_boot_and_unknown_layout_fail_closed(self):
        with patch.dict(os.environ,{'TOOLGATE_BOOTSTRAP_PASSWORD':'short'}):
            with self.assertRaises(ValueError):quickstart.initialize()
        self.assertFalse((quickstart.DATA/'identity.json').exists())
        quickstart.atomic(quickstart.DATA/'layout.json','{"layoutVersion":2}')
        with self.assertRaises(ValueError):quickstart.initialize()
    def test_duplicate_json_and_nonfinite_values_are_rejected(self):
        for body in ('{"password":"one","password":"two"}','{"value":NaN}','{"value":Infinity}'):
            with self.assertRaises(ValueError):quickstart.strict(body)
    def test_atomic_state_rejects_symlink(self):
        outside=self.root/'outside';outside.write_text('unchanged')
        (quickstart.DATA/'identity.json').symlink_to(outside)
        with self.assertRaises(ValueError):quickstart.atomic(quickstart.DATA/'identity.json','tampered')
        self.assertEqual(outside.read_text(),'unchanged')
    def test_consistent_backup_integrity_and_empty_restore_boundary(self):
        quickstart.atomic(quickstart.DATA/'layout.json','{"layoutVersion":1}')
        with sqlite3.connect(quickstart.DATA/'state/control.sqlite') as db:
            db.execute('PRAGMA journal_mode=WAL')
            db.execute('CREATE TABLE records(id INTEGER PRIMARY KEY,value TEXT)');db.execute("INSERT INTO records VALUES(1,'persistent')")
        backup=self.root/'snapshot';quickstart.backup(str(backup))
        target=self.root/'restored';target.mkdir();quickstart.DATA=target
        quickstart.restore(str(backup))
        with sqlite3.connect(target/'state/control.sqlite') as db:self.assertEqual(db.execute('SELECT value FROM records').fetchone()[0],'persistent')
        with self.assertRaises(ValueError):quickstart.restore(str(backup))
        empty=self.root/'empty';empty.mkdir();quickstart.DATA=empty
        (backup/'data/layout.json').write_text('tampered')
        with self.assertRaises(ValueError):quickstart.restore(str(backup))
        self.assertEqual(list(empty.iterdir()),[])


if __name__=='__main__':unittest.main()
