# Vision

AI assistants are becoming able to:

- read files
- change code
- operate infrastructure
- query databases
- control SaaS systems
- call internal APIs
- execute local software

The security question is no longer only:

> Can the user access the AI?

It is:

> What may the AI actually do?

OLO ToolGate exists to make that answer understandable and enforceable.

## We Want ToolGate to Make This Normal

```text
Engineering
  GitHub read                 ALLOW
  Create PR                   ALLOW
  Merge production PR        ASK
  Delete repository           BLOCK

Support
  CRM read                    ALLOW
  Customer export             ASK

AI running on unmanaged device
  privileged local tools     BLOCK
```

## We Also Want Tool Creation to Be Easy

An administrator or developer should be able to:

1. write a small Python/JS/PowerShell tool
2. describe when AI should use it
3. define inputs
4. define permissions
5. test it
6. deploy it internally
7. optionally publish it to the community

without building an entire MCP server from scratch.

## Long-Term Direction

MCP is the initial interoperability layer.

The architecture should later support:

```text
MCP
REST
OpenAPI
A2A
CLI
local executables
internal automation
```

ToolGate should become an **AI capability control plane**, not remain only an MCP proxy.
