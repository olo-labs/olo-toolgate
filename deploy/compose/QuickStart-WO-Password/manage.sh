#!/bin/sh
# Copyright 2026 OLO Labs
# SPDX-License-Identifier: Apache-2.0
set -eu
cd "$(dirname "$0")"
operation=${1:-deploy}
skip_pull=${2:-}
[ -f .env ] || cp .env.example .env
docker info >/dev/null
case "$operation" in
  undeploy) docker compose down; exit;;
  status) docker compose ps; exit;;
  logs) docker compose logs --tail 100; exit;;
  deploy|update) ;;
  *) echo 'Use deploy, update, undeploy, status or logs.' >&2; exit 2;;
esac
docker compose config --quiet
if [ "$skip_pull" != '--skip-pull' ]; then
  for image in $(docker compose config --images); do
    case "$image" in
      */*) docker pull "$image";;
      *) docker image inspect "$image" >/dev/null 2>&1 || {
           echo "Local-only image '$image' is missing. Build it first, or set QUICKSTART_IMAGE=ololab/olo-toolgate-quickstart:dev in .env." >&2; exit 1; };;
    esac
  done
fi
docker compose up -d --wait --wait-timeout 180
if [ -f check-options.py ]; then docker compose exec -T quickstart /opt/quickstart-python/bin/python /opt/quickstart/check-options.py; fi
echo 'Deployment ready. See README.md for external configuration and reviewed access.'
