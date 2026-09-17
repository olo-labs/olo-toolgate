# Scalability

## Gateway

- Stateless replicas.
- No DB request in normal authorization path.
- In-memory verified policy bundle.
- Scale behind load balancer.

## Control Plane

- Stateless replicas.
- External PostgreSQL.
- External artifact store/vault/optional cache.

## Marketplace

- Drupal xN.
- Marketplace API xN.
- Marketplace Worker xN.
- external DB/object store/queue/search/signing.

## Client Fleet

Use desired/reported state and batched heartbeats rather than keeping permanent server-side sessions for every endpoint.
