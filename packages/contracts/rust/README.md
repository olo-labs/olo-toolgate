# olo-toolgate-contracts (Rust)

Publishable Rust binding for canonical ToolGate contracts.

Within the monorepo, consumers should use a path/workspace dependency **with the version retained**:

```toml
olo-toolgate-contracts = { path = "../../packages/contracts/rust", version = "0.1.0" }
```

After a component moves to another repository:

```toml
olo-toolgate-contracts = "0.1"
```

Application imports remain unchanged.
