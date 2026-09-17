# Code Style, Comments, Copyright and License

## License

Apache-2.0.

## Source Header

Human-authored source files should include:

```text
Copyright 2026 OLO Labs
SPDX-License-Identifier: Apache-2.0
```

Example Java:

```java
/*
 * Copyright 2026 OLO Labs
 * SPDX-License-Identifier: Apache-2.0
 */
```

Rust:

```rust
// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
```

TypeScript:

```ts
// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
```

PHP:

```php
<?php
// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
```

Generated files may use a generator header instead.

## Comments

Comment:

```text
why
security reason
invariant
protocol nuance
concurrency nuance
non-obvious performance tradeoff
```

Do not comment obvious assignments.

## Naming

Prefer domain names:

```text
DeploymentAssignment
PolicyDecision
ResourceDescriptor
ExecutionPermit
PackageVersion
```

not vague names:

```text
Manager
Helper
Utils
Data
Processor
```

unless truly generic.

## Method Size

Keep methods small enough to review and test.

Refactor large conditional state logic into explicit state/policy objects.

## Immutability

Prefer immutable records/value objects for IDs, decisions, configuration snapshots and contracts.

## Exceptions

Do not use exceptions for expected control flow.

Map infrastructure exceptions to typed application failures at adapter boundaries.
