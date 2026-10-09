# Copyright 2026 OLO Labs
# SPDX-License-Identifier: Apache-2.0
"""Refuse to claim new options work with an older published image."""
import json
import os
import urllib.request

with urllib.request.urlopen('http://127.0.0.1:8080/api/quickstart/v1/status', timeout=5) as response:
    status = json.load(response)
expected = {'database':os.environ.get('TOOLGATE_QUICKSTART_DATABASE_MODE','sqlite'),
            'authority':'online',
            'passwordRequired':os.environ.get('TOOLGATE_DISABLE_ADMIN_PASSWORD','false') != 'true'}
if any(status.get(name) != value for name,value in expected.items()):
    raise SystemExit('Image does not implement selected database/online authority/login options. Use the updated Quickstart build/release.')
print('Database/online authority/login options verified.')
