# Contained Python tool example

From the repository root:

```sh
docker build -t toolgate-python-echo:dev examples/local-runtime-python
docker run --rm --network none --entrypoint /usr/local/bin/python3 toolgate-python-echo:dev --version
docker image inspect toolgate-python-echo:dev --format '{{.Id}}'
```

Use the printed exact Python version and local `sha256:<image ID>` in the protected
registration described in [runtime operations](../../docs/client/local-runtimes.md).
Local immutable image IDs are cache-only: they cannot be pulled from a registry.
For first-use provisioning, your reviewed image publication pipeline must publish
this image and supply its `repository@sha256:<manifest digest>`. Keep registry
credentials in that pipeline, never in a tool invocation or client archive.

Register `local.echo`/`execute`, `/opt/tool/tool.py`, and the closed text input/output
schemas from the guide. Enable the matching Gateway CUSTOM resource policy.
Then `olo-toolgate-client run local.echo '{"text":"hello"}'` uses the image's
Python even when the host has no Python. Host execution is never substituted.

This is source for an opt-in example, not a default installed or authorized tool.
