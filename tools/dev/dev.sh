#!/usr/bin/env sh
set -eu

cat <<'EOF'
OLO ToolGate development scaffold

The architecture/documentation repository is ready, but the runtime
implementation has not been bootstrapped yet.

Next implementation milestone:
  1. create apps/gateway
  2. create apps/control-plane
  3. create apps/admin-ui
  4. add compose.dev.yml
  5. make this command start the full local stack

Useful now:
  make docs
  make tree

Read:
  docs/getting-started/contributor-60-seconds.md
  ROADMAP.md
EOF
