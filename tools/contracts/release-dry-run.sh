#!/usr/bin/env sh
# Copyright 2026 OLO Labs
# SPDX-License-Identifier: Apache-2.0
set -eu
exec "${PYTHON:-python3}" tools/release/bundle.py "$@"
