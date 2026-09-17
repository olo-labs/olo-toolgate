# Modular Execution Plan

## Philosophy

Every module should leave the repository in a better, runnable state.

Avoid giant unreviewable jumps.

## Sequence

| Module | Purpose | Depends On |
|---|---|---|
| 00 | Foundation/contracts/build/CI | none |
| 01 | Gateway core | 00 |
| 02 | Control Plane backend | 00 |
| 03 | Admin UI | 02 |
| 04 | Policy bundle distribution | 01,02 |
| 05 | ASK approvals | 01,02,03 |
| 06 | Endpoint Client | 00,01,02 |
| 07 | HotFolder/built-ins | 06 |
| 08 | Local runtime | 06,07 |
| 09 | Package deployment | 02,06,08 |
| 10 | Tool Builder | 02,03,08,09 |
| 11 | Quickstart | 01,02,03,06,07 |
| 12 | Marketplace API | 00 |
| 13 | Marketplace Worker | 12 |
| 14 | Drupal | 12 |
| 15 | Marketplace integration | 02,03,12,13,14 |
| 16 | Production/Helm | all runtime services |
| 17 | Hardening | completed primary paths |
| 18 | GA | all |

## Rule

A module cannot rely on a future module to make its own primary path safe.

Example:

Gateway authorization cannot temporarily hardcode ALLOW until Control Plane exists.

Use valid static/dev inputs instead.
