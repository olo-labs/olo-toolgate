# Copyright 2026 OLO Labs
# SPDX-License-Identifier: Apache-2.0
"""Client ZIP archives are build inputs, not public Windows installers."""
def public_asset(path):
    name = path.name
    return not (name.startswith(('olo-toolgate-client-', 'olo-toolgate-chrome-')) and
                (name.endswith('.zip') or name.endswith('.zip.sha256')))
