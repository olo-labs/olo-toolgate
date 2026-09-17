# Errors and Idempotency

## Purpose

Mutations accept `Idempotency-Key`. Same key+same request returns same logical result; same key+different request returns conflict.
