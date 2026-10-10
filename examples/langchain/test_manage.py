# Copyright 2026 OLO Labs
# SPDX-License-Identifier: Apache-2.0
"""Group selection must retain enrollment, availability and platform boundaries."""
from copy import deepcopy
import json
from pathlib import Path
from types import SimpleNamespace
import tempfile
import unittest
from unittest.mock import patch

import manage


LINUX = 'device-' + 'a' * 32
WINDOWS = 'device-' + 'b' * 32
NOW = 1_800_000_000_000


def registration(device, platform):
    return dict(deviceId=device, platform=platform, gateway=manage.gateway_url(),
                profiles=[dict(tool=dict(id=name, enabled=True), extractor=dict(enabled=True))
                          for name in sorted(manage.TOOLS)])


def endpoint(device):
    return dict(deviceId=device, systemExecutor=False, directoryDevice=dict(enabled=True),
                endpointDevice=dict(state='ACTIVE', connectionApproved=True,
                                    lastSeenUnixMs=NOW - 1000, connectionExpiresAtUnixMs=NOW + 60000))


class Configuration:
    def __init__(self):
        self.group = dict(enabled=True, deviceIds=[LINUX, WINDOWS])
        self.rows = [endpoint(LINUX), endpoint(WINDOWS)]
        self.allocations = []

    def api(self, path):
        if path == '/api/control/v1/device-groups/' + manage.DEVICE_GROUP:
            return self.group
        if path == '/api/control/v1/endpoint/devices':
            return dict(items=self.rows)
        if path in ['/api/control/v1/devices/' + device + '/groups' for device in (LINUX, WINDOWS)]:
            return dict(groupIds=['existing-team-device-group'])
        raise AssertionError('Unexpected API operation: ' + path)

    def memberships(self, collection, device, groups):
        self.allocations.append((collection, device, groups))


class DeviceSelectionTests(unittest.TestCase):
    def setUp(self):
        self.config = Configuration()
        self.records = [registration(LINUX, 'linux'), registration(WINDOWS, 'windows')]
        clock = patch.object(manage.time, 'time', return_value=NOW / 1000)
        clock.start()
        self.addCleanup(clock.stop)

    def test_any_offers_both_group_members_to_selection(self):
        with patch.object(manage.secrets, 'choice', return_value=self.records[1]) as choice:
            self.assertEqual(manage.select_device(self.config, self.records, 'any')['deviceId'], WINDOWS)
            self.assertEqual(choice.call_args.args[0], self.records)

    def test_windows_filter_and_explicit_linux_filter(self):
        self.assertEqual(manage.select_device(self.config, self.records, 'windows')['deviceId'], WINDOWS)
        self.assertEqual(manage.select_device(self.config, self.records, 'linux')['deviceId'], LINUX)

    def test_unavailable_windows_falls_back_to_linux_only_for_any(self):
        self.config.rows[1]['endpointDevice']['lastSeenUnixMs'] = NOW - 120000
        self.assertEqual(manage.select_device(self.config, self.records, 'any')['deviceId'], LINUX)
        with self.assertRaisesRegex(ValueError, 'No available prepared windows'):
            manage.select_device(self.config, self.records, 'windows')

    def test_each_ineligible_state_is_excluded(self):
        changes = [
            ('directoryDevice', 'enabled', False),
            ('endpointDevice', 'connectionApproved', False),
            ('endpointDevice', 'state', 'REVOKED'),
            ('endpointDevice', 'connectionExpiresAtUnixMs', NOW),
            ('endpointDevice', 'lastSeenUnixMs', NOW + 1000),
            ('endpointDevice', 'lastSeenUnixMs', 0),
        ]
        for section, key, value in changes:
            with self.subTest(section=section, key=key):
                self.config.rows = [endpoint(LINUX), endpoint(WINDOWS)]
                self.config.rows[1][section][key] = value
                self.assertEqual(manage.select_device(self.config, self.records, 'any')['deviceId'], LINUX)

    def test_group_membership_registration_and_enabled_group_required(self):
        self.config.group['deviceIds'] = [LINUX]
        with self.assertRaisesRegex(ValueError, 'No available'):
            manage.select_device(self.config, self.records, 'windows')
        self.config.group['deviceIds'] = [WINDOWS]
        with self.assertRaisesRegex(ValueError, 'No available'):
            manage.select_device(self.config, self.records[:1], 'any')
        self.config.group['enabled'] = False
        with self.assertRaisesRegex(ValueError, 'must be enabled'):
            manage.select_device(self.config, self.records, 'any')

    def test_device_override_cannot_escape_group_or_os_filter(self):
        self.assertEqual(manage.select_device(self.config, self.records, 'any', WINDOWS)['deviceId'], WINDOWS)
        with self.assertRaisesRegex(ValueError, 'No available'):
            manage.select_device(self.config, self.records, 'windows', LINUX)
        self.config.group['deviceIds'] = [LINUX]
        with self.assertRaisesRegex(ValueError, 'No available'):
            manage.select_device(self.config, self.records, 'any', WINDOWS)

    def test_restart_requires_fresh_check_in_before_running_tools(self):
        before = endpoint(WINDOWS)['endpointDevice']
        after = {**before, 'lastSeenUnixMs': NOW}
        config = SimpleNamespace(api=lambda path: None)
        with patch.object(config, 'api', side_effect=[before, dict(enabled=True), after, dict(enabled=True)]) as api, \
                patch.object(manage.time, 'sleep') as sleep:
            manage.wait_device(config, WINDOWS, seen_after=NOW, timeout=360)
        self.assertEqual(api.call_count, 4)
        sleep.assert_called_once_with(2)

    def test_register_both_devices_preserving_existing_groups(self):
        with tempfile.TemporaryDirectory() as temporary:
            state = Path(temporary)
            (state / 'devices').mkdir()
            (state / 'devices' / (WINDOWS + '.json')).write_text(json.dumps(self.records[1]))
            args = SimpleNamespace(profiles=None, device_id=None, target='any', approve_linux_device=True)

            def private_file(path, value):
                path.parent.mkdir(parents=True, exist_ok=True)
                path.write_text(value)

            with patch.object(manage, 'STATE', state), \
                    patch.object(manage, 'linux_status', return_value=dict(deviceId=LINUX, state='ACTIVE')), \
                    patch.object(manage, 'docker', return_value=json.dumps(self.records[0]['profiles'])), \
                    patch.object(manage.shared, 'private_file', side_effect=private_file):
                records = manage.register_devices(self.config, args)
            self.assertEqual({record['deviceId'] for record in records}, {LINUX, WINDOWS})
            self.assertEqual(self.config.allocations, [
                ('devices', device, sorted(['existing-team-device-group', manage.DEVICE_GROUP]))
                for device in (LINUX, WINDOWS)])

    def test_foreign_gateway_registration_is_not_allocated(self):
        with tempfile.TemporaryDirectory() as temporary:
            state = Path(temporary)
            (state / 'devices').mkdir()
            foreign = deepcopy(self.records[1])
            foreign['gateway'] = 'https://localhost:18450'
            (state / 'devices' / (WINDOWS + '.json')).write_text(json.dumps(foreign))
            args = SimpleNamespace(profiles=None, device_id=None, target='any', approve_linux_device=False)
            with patch.object(manage, 'STATE', state), \
                    patch.object(manage, 'linux_status', return_value=dict(deviceId=LINUX, state='ACTIVE')), \
                    patch.object(manage, 'docker', return_value=json.dumps(self.records[0]['profiles'])), \
                    patch.object(manage.shared, 'private_file', side_effect=lambda path, value: path.write_text(value)):
                records = manage.register_devices(self.config, args)
            self.assertEqual([record['deviceId'] for record in records], [LINUX])
            self.assertEqual([row[1] for row in self.config.allocations], [LINUX])


if __name__ == '__main__':
    unittest.main()
