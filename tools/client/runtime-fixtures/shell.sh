#!/bin/bash
# Copyright 2026 OLO Labs
# SPDX-License-Identifier: Apache-2.0
# Pure built-ins only: the sandbox denies all child processes and never uses eval.
IFS= read -r input
input=${input/,\"toolId\":\"local.echo\"/}
input=${input/\"arguments\"/\"output\"}
# The harness sends mode along with text; remove its fixed echo field.
input=${input/\"mode\":\"echo\",/}
printf '%s\n' "$input"
