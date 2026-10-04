# Copyright 2026 OLO Labs
# SPDX-License-Identifier: Apache-2.0
"""Preserve runtime distribution notices and corresponding unmodified LGPL source."""
import hashlib
import importlib.metadata
from pathlib import Path
import shutil
import urllib.request

target = Path('/usr/share/licenses/olo-toolgate/quickstart-python')
target.mkdir(parents=True, exist_ok=True)
for name in ('cryptography','cffi','pycparser','redis','psycopg','typing_extensions'):
    distribution = importlib.metadata.distribution(name)
    destination = target/(name+'-'+distribution.version)
    destination.mkdir(exist_ok=True)
    for item in distribution.files or ():
        if 'license' in str(item).lower() or 'copying' in str(item).lower():
            source = Path(distribution.locate_file(item))
            if source.is_file(): shutil.copyfile(source, destination/source.name)
    (destination/'METADATA').write_text(distribution.read_text('METADATA') or '', encoding='utf-8')
url = 'https://files.pythonhosted.org/packages/76/26/3ea4ca5eaea1c0debcdf7ee7c1613fbe721dc27a03c461c0817ffd8a0601/psycopg-3.3.6.tar.gz'
with urllib.request.urlopen(url, timeout=30) as response: source = response.read(10*1024*1024)
if hashlib.sha256(source).hexdigest() != 'c081f2250df751a943036e42db6df4571c66cd0aabe8291a7a506512b12007d2':
    raise ValueError('Corresponding source checksum failed')
(target/'psycopg-3.3.6.tar.gz').write_bytes(source)
