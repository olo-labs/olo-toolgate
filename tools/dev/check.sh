#!/usr/bin/env sh
set -eu

printf '%s\n' 'Running first-commit scaffold checks...'

test -f README.md
test -f CONTRIBUTING.md
test -f SECURITY.md
test -f .gitignore
test -f docs/README.md
test -f docs/architecture/overview.md
test -f docs/reference/architecture-master.md
test -f docs/reference/marketplace-api-master.md

printf '%s\n' 'Scaffold checks passed.'
printf '%s\n' 'Language-specific checks will be added as components are initialized.'
