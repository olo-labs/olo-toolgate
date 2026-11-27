# Copyright 2026 OLO Labs; SPDX-License-Identifier: Apache-2.0
import importlib.util
from pathlib import Path
import sys
import subprocess
import unittest
from unittest.mock import patch
from tools.security import block_device

ROOT = Path(__file__).resolve().parents[2]
spec = importlib.util.spec_from_file_location("runtime_init", ROOT / "apps/tool-host/packaging/runtime-container/runtime-init.py")
runtime = importlib.util.module_from_spec(spec)
with patch.dict(sys.modules, {"block_device": block_device}):
    spec.loader.exec_module(runtime)


class RuntimeConfigurationTest(unittest.TestCase):
    def test_runtime_import_does_not_shadow_deployment_helpers(self):
        # A concrete security/deployment.py shadows the deployment namespace
        # package even when tools itself appears earlier on sys.path.
        script = """
import sys
from pathlib import Path
root = Path.cwd()
sys.path.insert(0, str(root / 'tests/ci'))
import test_tool_host_runtime_configuration
sys.path.insert(0, str(root / 'tools'))
import deployment.helm
"""
        subprocess.run([sys.executable, "-c", script], cwd=ROOT, check=True,
                       capture_output=True, text=True)

    def test_defaults_fit_the_images_paired_subordinate_identity_capacity(self):
        config = {}
        runtime.limits(config)
        self.assertEqual(4, config["limits"]["maxConcurrent"])
        self.assertEqual(268435456, config["limits"]["memoryBytes"])

    def test_operator_limits_are_preserved_and_excess_capacity_is_refused(self):
        limits = {"maxConcurrent": 2, "memoryBytes": 134217728}
        config = {"limits": limits.copy()}
        runtime.limits(config)
        self.assertEqual(limits, config["limits"])
        for concurrency in (0, -1, 33, 64, True, "4", None):
            with self.assertRaises(ValueError):
                runtime.limits({"limits": {"maxConcurrent": concurrency}})


if __name__ == "__main__":
    unittest.main()
