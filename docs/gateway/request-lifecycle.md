# Gateway Request Lifecycle

## Purpose

TLS → identity → agent/device → normalize tool/arguments → resource extraction → policy → approval/allow/block → credential resolution → upstream/local permit → audit → response.

## Required Tests

- Success path.
- Failure path.
- Security edge cases.
- Metrics/audit emitted.
