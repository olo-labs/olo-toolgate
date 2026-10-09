# Signed group snapshots and publication

Control owns the complete tenant graph, evaluator, durable invocation state and purpose-specific effect signer. Directory changes create transactional outbox events. Publication signs a new graph snapshot with a dedicated external snapshot key and a monotonic sequence; adoption records include directory revision, authorization epoch and graph digest.

Saved, Published and Adopted describe different durable facts. The console exposes publication and certificate-authenticated Device acknowledgements separately. Snapshots are metadata and discovery hints. Gateway and clients must contact current Core before every protected effect; snapshot possession, publication or green adoption never creates permission or permits offline ALLOW.

Snapshot keys cannot be reused for identity, effect, device or Fleet signing. Mount protected custody in Control only. Gateway has no snapshot verifier/evaluator fallback or approval signer. Use the [online Gateway configuration](../gateway/configuration.md) and [operational metrics and alerts](../enterprise-access-control/operations.md).

A restored database must advance external sequence/epoch/revision floors, retain spent nonce and retirement history, and quarantine authority. Rollback publishes a newer reviewed graph rather than replaying an old bundle. Old individual-policy/approval bundle contracts are archived fixtures only and are rejected by active bindings.
