# Run Your First Tool

First check the [device registry](../control-plane/device-registry.md).
Installed clients need an enabled registered owner/device and current timed or
unlimited connection approval. Quickstart's server tools use its registered fixed
executor and HotFolder identities. All paths still require applicable Gateway
filters and permits; installation, green status or approval alone grants no tool access.

## Built-In Tool

Open:

```text
Tools → Built-In → calculator.evaluate
```

Test:

```json
{
  "expression": "(12 * 5) + 7"
}
```

Expected:

```json
{
  "result": 67
}
```

## Local File Tool

After installing the endpoint client, place a file in HotFolder and call:

```text
hotfolder.list
hotfolder.read_text
```

The client must ask the Gateway for authorization before protected execution.

## Change Policy

Change:

```text
hotfolder.write_text
ALLOW → ASK
```

The next write should create an approval request.
