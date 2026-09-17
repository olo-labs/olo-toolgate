# toolgate-contracts — Java

Publishable Java binding for canonical ToolGate contracts.

Coordinates:

```text
io.ololabs.toolgate:toolgate-contracts
```

## Monorepo

Other Java modules use:

```kotlin
implementation(project(":contracts-java"))
```

## Split Repository

The same module can use:

```kotlin
implementation("io.ololabs.toolgate:toolgate-contracts:1.0.0")
```

Java imports do not change.

## Build

```bash
./gradlew :contracts-java:build
```

## Publish Locally

```bash
./gradlew :contracts-java:publishToMavenLocal
```

This allows us to test the separated-repository dependency path before actually splitting repositories.
