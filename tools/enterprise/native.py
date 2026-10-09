# Copyright 2026 OLO Labs
# SPDX-License-Identifier: Apache-2.0
"""Native enforcement, SDK and console checks using the shared pinned tool adapters."""
from pathlib import Path
import sys

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from check import run


def main():
    run(['cargo', 'fmt', '--all', '--check'])
    run(['cargo', 'test', '--workspace', '--locked'])
    run(['cargo', 'clippy', '--workspace', '--all-targets', '--locked', '--', '-D', 'warnings'])
    run(['npm', 'run', 'contracts:check'])
    run(['npm', 'run', 'contracts:build'])
    run(['npm', '--workspace', '@olo-labs/toolgate-contracts', 'test'])
    run(['npm', 'run', 'ui:check'])
    run(['npm', '--workspace', '@olo-labs/toolgate-admin-ui', 'test'])
    run(['php', 'packages/contracts/php/tests/roundtrip.php'])


if __name__ == '__main__':
    main()
