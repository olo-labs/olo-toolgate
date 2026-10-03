# ADR 012: Single-node Quickstart composition

Status: Accepted, 2026-10-04. Module 11 only.

The evaluation package reuses the actual Control JVM, embedded React console,
Rust Gateway and Rust fixed built-in executor in one non-root container. A bounded
Python supervisor owns process lifecycle, local identity, the AES-256-GCM vault
and same-origin HTTP composition. Author code never runs in this process or JVM.
Production services retain PostgreSQL/external identity/Vault and separate images.

An explicit Quickstart setting selects SQLite behind the existing Store interface.
Application use cases and canonical contracts remain unchanged. IMMEDIATE/WAL/FULL
transactions serialize local writes; checksummed ordered SQLite migrations and
append-only/immutable triggers retain audit, version and replay invariants. An
exclusive volume lock rejects a second instance. /data is service-owned mode 0700;
private keys and local identity hashes use 0600 atomic fsynced files. Never share
this state between replicas. No Quickstart Helm workload or horizontal scaling.

Bootstrap is a supplied strong password or a random credential in a private file,
never logs. First login requires a different strong password. PBKDF2-HMAC-SHA256
uses 600,000 iterations and random 32-byte salt. Signed 15-minute admin JWTs stay
in browser memory. Public composition checks persisted password generation,
exact local Host/Origin, bounded bodies/concurrency and login rate limits. Local
loopback HTTP is for evaluation; expose only a host-loopback published port.
Device enrollment retains direct TLS/mTLS and its independent generated CA.
Policy, permit, identity, TLS and device keys are separate generated RSA domains.

The vault encrypts values with AES-256-GCM and independent random nonces, authenticates
the secret name, and commits ciphertext plus mutation audit atomically in SQLite.
It exposes names and write operations only, never plaintext export. The separate
local key is protected by the volume's filesystem permissions: full volume theft
includes custody, so use encrypted host storage and protect backups. No automatic
credential binding for custom tools is introduced.

Defaults grant compute/read-only demo tools and exact welcome.txt operations;
mutations require ASK. Unselected paths/tools and deletion stay BLOCK. The fixed
executor obtains fresh Gateway decisions, consumes exact one-use approval permits,
and uses capability-relative HotFolder access. Runtime authorization is distinct
from admin sessions, assignment and testing. Failures cannot grant tool access.

SQLite JDBC is the pinned Xerial adapter (Apache-2.0), reusing JDBC rather than
adding another server/framework. Python cryptography is pinned to the existing
validated dependency version. Offline backup holds the data lock, uses SQLite
backup, includes private custody, and creates a checksummed manifest. Restore
requires an empty volume and validates every file. Newer layouts/checksum drift
reject startup; image rollback requires a compatible backup. Client native
logout/boot/ARM certification remains inherited, not claimed by this package.
