// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
import { readFileSync } from 'node:fs';
const request = JSON.parse(readFileSync(0, 'utf8'));
process.stdout.write(JSON.stringify({ protocolVersion: 1, requestId: request.requestId, output: { text: request.arguments.text } }));
