# Copyright 2026 OLO Labs
# SPDX-License-Identifier: Apache-2.0
"""Keep costly CI jobs behind the read-only metadata gate."""
from pathlib import Path
import unittest
import yaml

ROOT = Path(__file__).resolve().parents[2]


class EarlyGateTests(unittest.TestCase):
    def test_expensive_jobs_require_preflight(self):
        targets = {
            'foundation': ['check'],
            'gateway': ['policy-compatibility', 'container-and-cluster'],
            'control': ['clients', 'policy-compatibility', 'container-and-cluster'],
            'quickstart': ['clients', 'image'],
            'client': ['native', 'public-bundle'],
        }
        for workflow, names in targets.items():
            jobs = yaml.safe_load((ROOT/f'.github/workflows/{workflow}.yml').read_text())['jobs']
            self.assertEqual(jobs['preflight']['uses'], './.github/workflows/preflight.yml')
            def gated(name, seen=None):
                if name == 'preflight':
                    return True
                seen = (seen or set()) | {name}
                needs = jobs[name].get('needs', [])
                if isinstance(needs, str):
                    needs = [needs]
                return any(gated(parent, seen) for parent in needs if parent not in seen)
            for name in names:
                with self.subTest(workflow=workflow, job=name):
                    self.assertTrue(gated(name), 'Expensive job can start before metadata validation')
