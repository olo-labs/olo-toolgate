# Copyright 2026 OLO Labs; SPDX-License-Identifier: Apache-2.0
import importlib.util
import json
from pathlib import Path
from types import SimpleNamespace
import unittest

ROOT = Path(__file__).resolve().parents[2]
spec = importlib.util.spec_from_file_location("prepare_images", ROOT / "apps/tool-host/packaging/prepare-images.py")
images = importlib.util.module_from_spec(spec)
spec.loader.exec_module(images)


class PreparationTest(unittest.TestCase):
    def config(self):
        return {"sandbox": dict(podmanPath="/usr/bin/podman", **{
            key: "registry.example/toolgate/" + key + "@sha256:" + char * 64
            for key, char in (("pythonImage", "a"), ("brokerImage", "b"), ("jvmImage", "c"))})}

    def test_repeat_preparation_pulls_only_missing_exact_images(self):
        present = set()
        pulls = []
        def run(command, **kwargs):
            self.assertIn("--remote=false", command)
            self.assertNotIn("CONTAINER_HOST", kwargs["env"])
            args = command[4:]
            if args[0] == "info":
                body = {"host": {"security": {"rootless": True}, "serviceIsRemote": False}}
            elif args[:2] == ["image", "exists"]:
                return SimpleNamespace(returncode=0 if args[2] in present else 1)
            elif args[0] == "pull":
                present.add(args[1]); pulls.append(args[1]); body = {}
            else:
                body = [{"RepoDigests": [args[2]]}]
            return SimpleNamespace(returncode=0, stdout=json.dumps(body))
        images.prepare(self.config(), run)
        images.prepare(self.config(), run)
        self.assertEqual(3, len(pulls))
        self.assertTrue(all("@sha256:" in image for image in pulls))

    def test_mutable_images_are_rejected_before_engine_contact(self):
        for image in ("registry.example/java:21", "java@sha256:" + "a" * 64, "-bad@sha256:" + "a" * 64):
            config = self.config(); config["sandbox"]["jvmImage"] = image
            with self.assertRaises(ValueError):
                images.prepare(config, lambda *a, **kw: self.fail("engine contacted"))

    def test_remote_or_privileged_engine_is_rejected(self):
        for rootless, remote in ((False, False), (True, True)):
            def run(*args, **kwargs):
                return SimpleNamespace(stdout=json.dumps({"host": {"security": {"rootless": rootless}, "serviceIsRemote": remote}}))
            with self.assertRaises(ValueError): images.prepare(self.config(), run)

    def test_uncertain_presence_and_wrong_image_never_trigger_replacement(self):
        for exit_code in (0, 2):
            def run(command, **kwargs):
                args = command[4:]
                self.assertNotEqual("pull", args[0], "never replace an indeterminate or mismatched image")
                if args[0] == "info":
                    body = {"host": {"security": {"rootless": True}, "serviceIsRemote": False}}
                elif args[:2] == ["image", "exists"]:
                    return SimpleNamespace(returncode=exit_code)
                else:
                    body = [{"Digest": "sha256:" + "f" * 64, "RepoDigests": []}]
                return SimpleNamespace(returncode=0, stdout=json.dumps(body))
            with self.assertRaises((ValueError, RuntimeError)): images.prepare(self.config(), run)


if __name__ == "__main__":
    unittest.main()
