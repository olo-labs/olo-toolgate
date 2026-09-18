#!/usr/bin/env sh
set -eu

required="
README.md
CONTRIBUTING.md
ARCHITECTURE.md
VISION.md
ROADMAP.md
SECURITY.md
docs/README.md
docs/INDEX.md
docs/architecture/overview.md
docs/security/security-invariants.md
"

for f in $required; do
  if [ ! -f "$f" ]; then
    printf 'Missing required documentation: %s\n' "$f" >&2
    exit 1
  fi
done

# Catch documentation links to files that do not exist in the checkout. Keep
# external URLs untouched; this lightweight check runs without extra tools.
if command -v python3 >/dev/null 2>&1; then
  python3 - <<'PY'
import pathlib
import re
import sys

root = pathlib.Path('.')
missing = []
for path in root.rglob('*.md'):
    if any(part in {'.git', 'node_modules', 'target'} for part in path.parts):
        continue
    for target in re.findall(r'\[[^]]+\]\(([^)]+)\)', path.read_text(encoding='utf-8', errors='ignore')):
        target = target.split('#', 1)[0]
        if not target or '://' in target or target.startswith('mailto:'):
            continue
        if not (path.parent / target).resolve().exists():
            missing.append(f'{path}: {target}')
if missing:
    print('Missing Markdown targets:', file=sys.stderr)
    print('\n'.join(missing), file=sys.stderr)
    sys.exit(1)
PY
fi

printf '%s\n' 'Documentation structure check passed.'
