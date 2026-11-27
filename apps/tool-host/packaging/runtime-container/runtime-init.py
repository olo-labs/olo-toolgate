#!/usr/bin/env python3
# Copyright 2026 OLO Labs; SPDX-License-Identifier: Apache-2.0
"""Initialize only this container's dedicated volume, then start managed custody."""
import json
import os
from pathlib import Path
from block_device import io_device_for_storage

STATE = Path("/var/lib/toolgate-tool-host")
STORAGE = '[storage]\ndriver="overlay"\n[storage.options.overlay]\nmount_program="/usr/bin/fuse-overlayfs"\n'


def limits(config):
    """The image provisions 65,536 subordinate IDs, enough for 32 paired slots."""
    value = config.setdefault("limits", dict(maxConcurrent=4, maxInputBytes=1048576,
        maxOutputBytes=4194304, syncDeadlineMs=60000, memoryBytes=268435456,
        maxPermitWindowMs=60000))
    concurrency = value.get("maxConcurrent")
    if type(concurrency) is not int or not 1 <= concurrency <= 32:
        raise ValueError("runtime container requires 1..32 paired slots")


def main():
    if os.geteuid() != 0:
        raise SystemExit("container init requires root; the Tool Host service runs unprivileged")
    STATE.mkdir(mode=0o700, exist_ok=True)
    if STATE.is_symlink() or STATE.stat().st_uid not in (0, 1000):
        raise SystemExit("foreign Tool Host state volume")
    if STATE.stat().st_uid == 0:
        if any(STATE.iterdir()):
            raise SystemExit("refusing a nonempty root-owned state volume")
        STATE.chmod(0o700)
        os.chown(STATE, 1000, 1000)
    if STATE.stat().st_mode & 0o777 != 0o700:
        raise SystemExit("state volume must be private (0700); preserved unchanged")
    for path in (STATE / ".config", STATE / ".config/containers"):
        if not path.exists():
            path.mkdir(mode=0o700)
            os.chown(path, 1000, 1000)
        if path.is_symlink() or path.stat().st_uid != 1000 or path.stat().st_mode & 0o777 != 0o700:
            raise SystemExit("foreign rootless storage configuration")
    storage = STATE / ".config/containers/storage.conf"
    if storage.exists():
        if storage.is_symlink() or storage.read_text() != STORAGE:
            raise SystemExit("unsupported rootless storage configuration; preserved unchanged")
    else:
        with storage.open("x") as stream:
            stream.write(STORAGE)
        storage.chmod(0o600)
        os.chown(storage, 1000, 1000)
    config = json.loads(Path("/etc/toolgate/tool-host.json").read_text(encoding="utf-8"))
    limits(config)
    sandbox = config["sandbox"]
    # A named volume exposes the actual backing device. Overlay/stacked storage
    # requires an independently verified override; never guess from a sample ID.
    sandbox["ioDevice"] = io_device_for_storage(STATE, sandbox.get("ioDevice") or None)
    sandbox["podmanPath"] = "/usr/bin/podman"
    sandbox["cgroupParent"] = "/system.slice/olo-toolgate-tool-host.service"
    config["storeDir"] = str(STATE / "store")
    config["workDir"] = str(STATE / "work")
    if "enrollment" in config:
        config["enrollment"]["identityDir"] = str(STATE / "identity")
    effective = Path("/run/toolgate-config")
    effective.mkdir(mode=0o750, exist_ok=True)
    if effective.is_symlink() or effective.stat().st_uid != 0:
        raise SystemExit("foreign effective configuration directory")
    os.chown(effective, 0, 1000)
    path = effective / "tool-host.json"
    if path.is_symlink():
        raise SystemExit("effective configuration must not be a symlink")
    path.write_text(json.dumps(config) + "\n", encoding="utf-8")
    path.chmod(0o640)
    os.chown(path, 0, 1000)
    os.execv("/sbin/init", ["/sbin/init"])


if __name__ == "__main__":
    main()
