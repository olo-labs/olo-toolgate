#!/usr/bin/env sh
set -eu

printf '%s\n' 'OLO ToolGate contracts release dry run'
printf '%s\n' ''
cat packages/contracts/contract-set.yaml

printf '%s\n' ''
printf '%s\n' 'Expected publication targets:'
printf '%s\n' '  Rust:     olo-toolgate-contracts'
printf '%s\n' '  Maven:    io.ololabs.toolgate:toolgate-contracts'
printf '%s\n' '  npm:      @olo-labs/toolgate-contracts'
printf '%s\n' '  Composer: olo-labs/toolgate-contracts'
printf '%s\n' '  Raw:      versioned schema/OpenAPI/event bundle'
printf '%s\n' ''
printf '%s\n' 'No artifact was published.'
