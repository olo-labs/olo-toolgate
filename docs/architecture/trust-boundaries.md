# Trust Boundaries

Managed tool execution uses a trusted registered device, current owner/device enablement, installed-client connection approval, and the existing agent/tool/action/resource filters before every protected effect. Registration, green availability or deployment never grants execution. Device identity comes from a verified TLS peer or composition-owned credential, never an asserted header or tool argument.

See [Device registry and tool-call controls](../control-plane/device-registry.md).

## Marketplace Trust

Marketplace signature means:

> This exact package/version is an immutable Marketplace release.

It does **not** mean:

> This organization should deploy it.

## Organization Trust

Organization deployment signature means:

> This package/version is assigned to this managed client.

It does **not** mean:

> Every runtime action is allowed.

## Runtime Trust

Gateway permit means:

> This exact user/agent/device/tool/resource/request is authorized now.

## Boundary Chain

```text
Marketplace release trust
        +
Organization deployment trust
        +
Runtime Gateway authorization
        =
Managed execution
```

Uploaded code is always untrusted until isolated and validated.

Module 10 authoring never executes uploaded code in Control. Only an explicitly
designated enrolled endpoint receives an organization-signed, expiring mTLS test
lease. Tests are confined compute probes and cannot assign a tool or grant a
runtime permit. Sealed package releases still require an independent release
authority and signed fleet assignment. Inline invocation approval scopes include
the full runtime/tool registration digest, so a source update changes the scope.
