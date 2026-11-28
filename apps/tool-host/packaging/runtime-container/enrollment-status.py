#!/usr/bin/env python3
# Copyright 2026 OLO Labs; SPDX-License-Identifier: Apache-2.0
"""Public enrollment evidence for the installation operator, never execution authority."""
import hashlib
import json
import os
from pathlib import Path
import subprocess


def status(directory, config):
    journal = directory / "device.json"
    key = directory / "device-key.pem"
    if directory.is_symlink() or journal.is_symlink() or key.is_symlink():
        raise SystemExit("unsafe identity path")
    state = json.loads(journal.read_text())
    if (state.get("formatVersion") != 3 or state.get("tenantId") != config["control"]["tenantId"]
            or state.get("serverUrl") != config["control"]["url"]):
        raise SystemExit("unsupported or foreign identity; preserved unchanged")
    public = subprocess.run(["/usr/bin/openssl", "pkey", "-in", str(key), "-pubout", "-outform", "DER"],
                            capture_output=True, check=True, timeout=5).stdout
    return dict(hostId=config["hostId"], tenantId=state["tenantId"],
        userCode=(state.get("challenge") or {}).get("userCode"),
        keyFingerprint=hashlib.sha256(public).hexdigest())


def main():
    if os.geteuid() != 1000:
        raise SystemExit("run as the dedicated Tool Host user (1000:1000)")
    config = json.loads(Path("/run/toolgate-config/tool-host.json").read_text())
    print(json.dumps(status(Path("/var/lib/toolgate-tool-host/identity"), config)))


if __name__ == "__main__":
    main()
