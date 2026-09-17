# Common Repository Commands

The repo should expose stable top-level commands.

```bash
make dev            # start contributor environment
make stop           # stop local environment
make check          # formatting/lint/unit/schema checks
make test           # all unit tests
make integration    # integration tests
make e2e            # end-to-end tests
make security       # security test suite
make benchmark      # performance benchmark suite
make containers     # build Docker images
make client         # build endpoint client
make docs           # validate docs links/examples
make clean          # clean generated local state
```

Avoid requiring contributors to memorize language-specific commands for normal workflows.
