# Contributor Checklist

Before coding:

- [ ] Read `getting-started/contributor-60-seconds.md`.
- [ ] Identify component ownership in `architecture/component-map.md`.
- [ ] Check whether an ADR is required.
- [ ] Check canonical schema impact.
- [ ] Identify security invariants affected.

Before PR:

- [ ] `make check`
- [ ] Unit tests added/updated.
- [ ] Integration/E2E tests added where cross-component behavior changed.
- [ ] Security tests added for authorization/trust changes.
- [ ] Docs updated.
- [ ] No secrets/log-sensitive values.
- [ ] Generated schema bindings current.
- [ ] Compatibility impact documented.
