#!/usr/bin/env sh
set -eu

printf '%s\n' 'Running repository scaffold checks...'

test -f README.md
test -f CONTRIBUTING.md
test -f SECURITY.md
test -f .gitignore
test -f settings.gradle.kts
test -f build.gradle.kts
test -f docs/README.md
test -f docs/architecture/overview.md
test -f docs/reference/architecture-master.md
test -f docs/reference/marketplace-api-master.md

printf '%s\n' 'Static scaffold checks passed.'

if [ "${TOOLGATE_FULL_CHECK:-0}" = "1" ]; then
  ./tools/contracts/check.sh
else
  printf '%s\n' 'Set TOOLGATE_FULL_CHECK=1 to run toolchain-dependent contract checks.'
fi
