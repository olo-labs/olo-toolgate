# Copyright 2026 OLO Labs
# SPDX-License-Identifier: Apache-2.0
"""Every canonical reference must resolve locally for all language runtimes."""
import json
from pathlib import Path
import unittest

ROOT=Path(__file__).resolve().parents[2]

class ReferenceIntegrity(unittest.TestCase):
    def test_every_reference_has_a_local_file_and_pointer(self):
        schemas={p.name:json.loads(p.read_text(encoding='utf-8')) for p in (ROOT/'packages/contracts/schemas/v1').glob('*.json')}
        def visit(value,filename):
            if isinstance(value,dict):
                if '$ref' in value:
                    file,_,fragment=value['$ref'].partition('#')
                    self.assertIn(file or filename,schemas,(filename,value['$ref']))
                    target=schemas[file or filename]
                    for part in fragment.split('/')[1:]:
                        part=part.replace('~1','/').replace('~0','~')
                        self.assertIsInstance(target,dict,(filename,value['$ref']))
                        self.assertIn(part,target,(filename,value['$ref']))
                        target=target[part]
                for child in value.values():visit(child,filename)
            elif isinstance(value,list):
                for child in value:visit(child,filename)
        for filename,schema in schemas.items():visit(schema,filename)

if __name__=='__main__':unittest.main()
