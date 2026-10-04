# Directory users, teams and default policies

New users created through the Control directory API (including the console) are
automatically added to the configured default team. The console defaults new
records to disabled. An administrator can enable the user after reviewing the
identity. Default-team membership does not enable a disabled user.

Open **Users → Teams**, select a team, edit its comma-separated **User IDs**, and save to
allocate membership. The same user ID can appear in several teams. Removing a
user from the default team does not remove their other memberships; existing
users are not automatically re-added on updates or restart. Reload the record
after a revision conflict before applying another change.

Policies select teams through their `teamIds` field. Members inherit those
scopes when they are enabled and have the applicable
[privilege templates](../security/user-roles.md). Disabled members remain visible in the team,
but are excluded from compiled team scopes. A team with no enabled members does
not compile into an unrestricted rule. Other policy restrictions (tool, action,
resource, agent and device), BLOCK precedence and ASK still apply.

Quickstart seeds its built-in default policies with `team-default` as a scope.
New-user registration also links existing configured default policies to that
team, preserving their decision and other restrictions. This supports retained
Quickstart data whose earlier default policies did not select the default team.
Membership, policy scope changes, audit records and the new user commit in one
transaction. Retrying the same request with its idempotency key does not add
duplicate memberships or increment revisions again.

For a separate Control deployment, configure:

```dotenv
TOOLGATE_DEFAULT_TEAM_ID=team-default
TOOLGATE_DEFAULT_POLICY_IDS=organization-default-read,organization-default-write
```

The team is created on first user registration if absent. Default policies must
be real, administrator-created policy records; the server does not invent tool
definitions or grant rules. Missing configured policy records confer no access.
Quickstart supplies its exact built-in policy IDs by default. Administrators can
override that list for their own defaults. A deleted default-team identifier
cannot be reused: configure another team ID before registering additional users.

After changing membership, activation or policies, publish a new signed policy
bundle for Gateways to receive the change. Existing signed bundles remain valid
under their normal version/expiry rules until replaced. Bulk configuration
import preserves the supplied membership graph rather than applying registration
defaults; include default-team membership explicitly when importing new users.

Users, Teams, Tools, Policies, Agents and Clients have editors with revision
checks, disabled defaults, and confirmed deletion. The server validates schemas,
references and administrator authorization. A referenced user cannot be deleted
until the administrator removes its team/policy references and dependent records.

Manage fixed templates and JSON device/tool scope on named **Users → Roles**.
Assign role IDs on users or teams; multiple assignments combine. Role-bearing
team changes and role assignments require Super Admin. See
[managed roles](managed-roles.md) for examples and upgrade behavior.

The Endpoint client installation section appears only on the login screen and
Enroll Device page. Other authenticated pages do not repeat the download section.

Verification: `SqliteTest` exercises transactional enrollment, retries, multiple
teams, stale edits, policy inheritance and rollback. `PolicyBundleTest` verifies
disabled members and empty enabled membership cannot become grants. UI tests
exercise team editing and concurrency/idempotency headers.
