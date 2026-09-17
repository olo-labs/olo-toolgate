# First Login

Quickstart provisions one local administrator.

## First Login Rules

- No universal default password.
- If `QUICKSTART_ADMIN_PASSWORD` is set, use that value for bootstrap.
- Otherwise generate a random one-time bootstrap password and print it once.
- Prompt the user to change it.
- Create `Personal Workspace`.
- Create `Local User` team.
- Create `Interactive AI` agent profile.
- Activate Quickstart Safe Defaults.

Enterprise mode should use OIDC and should not depend on local passwords except emergency/bootstrap flows.
