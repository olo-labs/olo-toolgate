# Configuration and Secrets

## Config

Typed and validated.

Sources may include:

```text
environment
config file
Kubernetes ConfigMap
admin-managed settings
```

Precedence must be documented.

## Secrets

Referenced, not embedded.

Example:

```text
secret://github/team-token
```

## Production Helm

Values should refer to:

```text
existingSecret
secretKeyRef
external-secret integration
```

Never put production credentials in `values.yaml`.

## Startup

Missing required secret/config => clear failure or degraded behavior exactly as documented.

Never silently substitute insecure defaults.
