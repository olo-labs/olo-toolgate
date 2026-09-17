#!/usr/bin/env sh
set -eu

printf '%s\n' 'Checking shared contract package structure...'

required="
packages/contracts/VERSION
packages/contracts/contract-set.yaml
packages/contracts/schemas/README.md
packages/contracts/openapi/README.md
packages/contracts/events/README.md
packages/contracts/rust/Cargo.toml
packages/contracts/java/build.gradle.kts
packages/contracts/typescript/package.json
packages/contracts/php/composer.json
settings.gradle.kts
build.gradle.kts
"

for f in $required; do
  test -f "$f" || {
    printf 'Missing contract/build file: %s\n' "$f" >&2
    exit 1
  }
done

if command -v cargo >/dev/null 2>&1; then
  printf '%s\n' 'Rust contracts: cargo check'
  cargo check -p olo-toolgate-contracts
else
  printf '%s\n' 'Rust contracts: skipped (cargo not installed)'
fi

if [ "${TOOLGATE_SKIP_GRADLE_BOOTSTRAP:-0}" = "1" ]; then
  printf '%s\n' 'Java contracts: skipped by TOOLGATE_SKIP_GRADLE_BOOTSTRAP=1'
else
  printf '%s\n' 'Java contracts: Gradle compile/check'
  ./gradlew --no-daemon :contracts-java:check
fi

printf '%s\n' 'Contract structure check passed.'
