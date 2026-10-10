#!/bin/bash
# Copyright 2026 OLO Labs
# SPDX-License-Identifier: Apache-2.0
# usage: run_legs.sh <name> <server-cwd> <url> <rev> <server command...>
NAME=$1; CWD=$2; URL=$3; REV=$4; shift 4
OUT=/tmp/claude-0/conformance/work/results/$NAME-$REV
mkdir -p $OUT
CONF=/tmp/claude-0/conformance/work/conf/node_modules/.bin/conformance
( cd "$CWD" && exec "$@" ) > $OUT/server.log 2>&1 &
PID=$!
for i in $(seq 1 120); do curl -s --max-time 2 -o /dev/null "$URL" && break; kill -0 $PID 2>/dev/null || { echo "server died"; tail -30 $OUT/server.log; exit 1; }; sleep 0.5; done
cd /tmp/claude-0/conformance/work
$CONF server --url "$URL" --requirements $REV -o $OUT/checks > $OUT/conformance.txt 2>&1
echo "exit=$?" >> $OUT/conformance.txt
kill $PID; sleep 1; pkill -P $PID 2>/dev/null; kill -9 $PID 2>/dev/null
tail -60 $OUT/conformance.txt
