# Gateway Performance

## Purpose

Target very low added latency, small memory, fast startup and no DB access in the typical request path. Publish benchmark results before claims.

Run `make benchmark` or `cargo bench -p olo-toolgate-gateway --bench authorization
--locked`. The release harness warms 1,000 calls and measures 20,000, reporting
p50/p95/p99/mean nanoseconds. It includes validation, clone, extraction, hashing,
policy and audit event construction. The audit adapter acknowledges immediately:
network, TLS, body transfer, collector I/O and durable storage are excluded.

Record CPU/OS/toolchain, sample count and limits. Use a quiet fixed environment
and retain build/gateway/benchmark.json. Measurements are observations, not
production guarantees. Never gate correctness on noisy latency thresholds.
Limit tests use deterministic clocks. See the [completion report](../codex/modules/01-completion.md)
for the measured baseline and container evidence.

The path reads immutable memory without DB/Control/Marketplace queries or retained
per-user sessions. Body/header/concurrency/connection/rate/audit bounds protect
resources. Replicas need identical snapshots; per-replica capacity limits scale
with replica count and provide no fleet quota or distributed replay guarantee.

## Required Tests

Module 01 baseline (2026-10-02): 20,000 samples after 1,000 warmups, Rust 1.94.1
release in Docker `rust:1.94-bookworm` on Windows 11, Intel Family 6 Model 85
Stepping 4: p50 8,909 ns, p95 28,418 ns, p99 37,900 ns, mean 12,358 ns.
This measures the authorization core with immediate audit acknowledgement; it
does not measure the production musl image, network or collector throughput.

- Success path.
- Failure path.
- Security edge cases.
- Metrics/audit emitted.

## Module 04 signed evaluation baseline

`make benchmark` records `build/gateway/bundle-benchmark.json` separately from
the core baseline. One genuine signed 512-rule snapshot is verified before
measurement; the last matching rule is BLOCK, forcing a complete scan.
The 1,000 warmups/20,000 samples use a fixed UTC/monotonic clock and assert
matched versioned decisions, so fixture expiry cannot change the measured path.
This microbenchmark includes snapshot acquisition, exact matching and allocation;
it excludes production clock sampling, extraction, schemas, network and audit I/O.
Cold signature/hash/schema adoption is measured separately.

The 2026-10-02 Rust 1.94.1 release run in Docker on Windows 11 (Intel Family 6
Model 85 Stepping 4) measured p50 9,703 ns, p95 39,251 ns, p99 60,599 ns and
mean 15,864 ns; cold adoption was 9,349,243 ns. Results vary with host load and
are not end-to-end throughput/latency promises. No noisy performance threshold
is a correctness gate. Evaluation remains bounded by the signed 4,096-rule
contract; the current Control directory limit bounds compiler output to 512.
