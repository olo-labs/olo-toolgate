#!/usr/bin/env python3
# Copyright 2026 OLO Labs; SPDX-License-Identifier: Apache-2.0
"""Prepare pinned images before service startup, under the dedicated rootless account.

No source compilation, tag resolution, Control qualification or admission occurs here.
"""
import json
import os
from pathlib import Path, PurePosixPath
import re
import subprocess
import sys

PIN = re.compile(r"[A-Za-z0-9][A-Za-z0-9./_:-]*@sha256:[a-f0-9]{64}")


def prepare(config, run=subprocess.run):
    sandbox = config["sandbox"]
    engine = sandbox["podmanPath"]
    if not PurePosixPath(engine).is_absolute():
        raise ValueError("absolute local Podman executable required")
    images = [sandbox["pythonImage"], sandbox["brokerImage"]]
    if sandbox.get("jvmImage") is not None:
        images.append(sandbox["jvmImage"])
    if any(not isinstance(image, str) or not PIN.fullmatch(image) or "/" not in image.split("@")[0]
           for image in images):
        raise ValueError("every image must be a fully qualified immutable reference")
    env = {key: os.environ[key] for key in ("HOME", "XDG_RUNTIME_DIR", "DBUS_SESSION_BUS_ADDRESS") if key in os.environ}
    env["PATH"] = "/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin"
    command = [engine, "--remote=false", "--runtime=crun", "--cgroup-manager=cgroupfs"]

    def execute(args, check=True):
        return run(command + args, env=env, check=check, capture_output=True, text=True, timeout=300)

    info = json.loads(execute(["info", "--format=json"]).stdout)["host"]
    if info["security"]["rootless"] is not True or info["serviceIsRemote"] is not False:
        raise ValueError("local rootless Podman required")
    for image in dict.fromkeys(images):
        result = execute(["image", "exists", image], check=False)
        if result.returncode == 1:
            execute(["pull", image])
        elif result.returncode != 0:
            raise RuntimeError("image presence is indeterminate; refusing preparation")
        metadata = json.loads(execute(["image", "inspect", image]).stdout)[0]
        if metadata.get("Digest") != image.split("@")[1] and image not in metadata.get("RepoDigests", []):
            raise ValueError("prepared image does not match its configured pin")


def main():
    if sys.platform != "linux" or os.geteuid() == 0:
        raise SystemExit("run on Linux as the dedicated unprivileged Tool Host account")
    path = Path(os.environ["TOOLGATE_TOOL_HOST_CONFIG"])
    prepare(json.loads(path.read_text(encoding="utf-8")))


if __name__ == "__main__":
    main()
