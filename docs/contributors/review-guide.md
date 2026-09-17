# Review Guide

Review for:

1. correctness
2. security boundary
3. failure behavior
4. tests
5. compatibility
6. observability
7. documentation

For security-sensitive code ask:

```text
What happens when this dependency is down?
What happens on malformed input?
Can this broaden access?
Can this leak a credential?
Can this bypass signature validation?
Is failure BLOCK or ALLOW?
```
