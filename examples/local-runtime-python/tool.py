# Copyright 2026 OLO Labs
# SPDX-License-Identifier: Apache-2.0
"""Minimal standard invocation example: no shell, network or host filesystem."""
import json
import sys

request = json.loads(sys.stdin.buffer.read(65537))
print(json.dumps({'protocolVersion': 1, 'requestId': request['requestId'],
                  'output': {'text': request['arguments']['text']}}))
