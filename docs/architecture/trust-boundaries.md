# Trust Boundaries

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
