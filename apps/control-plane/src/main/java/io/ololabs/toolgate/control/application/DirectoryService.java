// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
package io.ololabs.toolgate.control.application;

import io.ololabs.toolgate.control.domain.Directory;
import io.ololabs.toolgate.control.domain.Ids;
import io.ololabs.toolgate.control.domain.Ids.Kind;
import io.ololabs.toolgate.control.domain.Ids.TenantId;
import io.ololabs.toolgate.contracts.ErrorCode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

/** Administrative use cases. Authorization is rechecked before reading an idempotency response. */
public final class DirectoryService {
    private final Store store;
    private final Codec codec;
    private final int maxRecords;
    private final int maxBytes;
    private final String defaultTeamId;
    private final java.util.List<String> defaultPolicyIds;
    public DirectoryService(Store store, Codec codec, int maxRecords, int maxBytes) {
        this(store, codec, maxRecords, maxBytes, "");
    }
    public DirectoryService(Store store, Codec codec, int maxRecords, int maxBytes, String defaultTeamId) {
        this(store, codec, maxRecords, maxBytes, defaultTeamId, java.util.List.of());
    }
    public DirectoryService(Store store, Codec codec, int maxRecords, int maxBytes, String defaultTeamId, java.util.List<String> defaultPolicyIds) {
        this.store = store; this.codec = codec; this.maxRecords = maxRecords; this.maxBytes = maxBytes;
        this.defaultTeamId = defaultTeamId.isEmpty() ? "" : Ids.valid(defaultTeamId);
        this.defaultPolicyIds = defaultPolicyIds.stream().map(Ids::valid).distinct().sorted().toList();
        if (defaultTeamId.isEmpty() && !this.defaultPolicyIds.isEmpty()) throw new IllegalArgumentException("Default policies require a default team");
    }
    public record Actor(TenantId tenant, String id, boolean admin, boolean superAdmin) {
        public Actor(TenantId tenant, String id, boolean admin) { this(tenant,id,admin,false); }
        public Actor {
            java.util.Objects.requireNonNull(tenant);
            if (id == null || !id.matches("[a-f0-9]{64}")) throw Failure.validation();
        }
        public void requireAdmin() {
            if (!admin) throw new Failure(ErrorCode.FORBIDDEN, 403, "Administrator role required");
        }
    }
    /** Verified identity roles and persisted directory activation are both required for portal access. */
    public void requirePortal(Actor actor, String userId) {
        actor.requireAdmin();
        if (userId == null) return; // Existing external IdP principals use verified JWT groups until explicitly bound.
        store.transaction(actor.tenant(), false, tx -> {
            var entry = tx.load().entries().get(Kind.USER.id(userId));
            if (entry == null || !entry.enabled()) throw new Failure(ErrorCode.FORBIDDEN,403,"Portal access denied");
            var user = codec.model(entry.document(),io.ololabs.toolgate.contracts.ControlUser.class);
            var resolved=new RoleResolver(codec).resolve(tx.load(),user);
            if ((user.access()!=null || resolved.managed()) && resolved.portalRole() == io.ololabs.toolgate.contracts.UserRole.BASIC)
                throw new Failure(ErrorCode.FORBIDDEN,403,"Administrator role required");
            return null;
        });
    }
    public boolean portalSuper(Actor actor, String userId) {
        if (!actor.superAdmin() || userId == null) return actor.superAdmin();
        return store.transaction(actor.tenant(),false,tx->{
            var entry=tx.load().entries().get(Kind.USER.id(userId));
            if(entry==null || !entry.enabled()) return false;
            var user=codec.model(entry.document(),io.ololabs.toolgate.contracts.ControlUser.class);
            var resolved=new RoleResolver(codec).resolve(tx.load(),user);
            return user.access()==null && !resolved.managed() || resolved.portalRole()==io.ololabs.toolgate.contracts.UserRole.SUPER_ADMIN;
        });
    }
    private void protectAssignments(Actor actor, Directory.Entry old, Directory.Entry input) {
        var entry=input==null?old:input;
        if(entry==null) return;
        if(entry.id().kind()==Kind.ROLE) {
            if(!actor.superAdmin()) throw new Failure(ErrorCode.FORBIDDEN,403,"Super administrator role required");
            return;
        }
        if(entry.id().kind()!=Kind.USER && entry.id().kind()!=Kind.TEAM) return;
        var before=assignedRoles(old); var after=assignedRoles(input);
        // Changing enabled state or members of a role-bearing team also changes effective grants.
        boolean teamGrant=entry.id().kind()==Kind.TEAM && (!before.isEmpty() || !after.isEmpty());
        if((!before.equals(after) || teamGrant) && !actor.superAdmin())
            throw new Failure(ErrorCode.FORBIDDEN,403,"Super administrator role required");
    }
    private java.util.List<String> assignedRoles(Directory.Entry entry) {
        if(entry==null) return java.util.List.of();
        java.util.List<String> ids;
        if(entry.id().kind()==Kind.USER) {
            var user=codec.model(entry.document(),io.ololabs.toolgate.contracts.ControlUser.class);
            ids=user.access()==null?null:user.access().roleIds();
        } else ids=codec.model(entry.document(),io.ololabs.toolgate.contracts.ControlTeam.class).roleIds();
        return ids==null?java.util.List.of():ids.stream().sorted().toList();
    }
    private void protectRole(Actor actor, Directory.Entry old, Directory.Entry input, Directory before) {
        var previous = old == null ? null : codec.model(old.document(),io.ololabs.toolgate.contracts.ControlUser.class);
        var next = input == null ? null : codec.model(input.document(),io.ololabs.toolgate.contracts.ControlUser.class);
        var previousTemplates=previous==null || previous.access()==null?java.util.List.of():previous.access().templateIds();
        var nextTemplates=next==null || next.access()==null?java.util.List.of():next.access().templateIds();
        var previousGroups=previous==null || previous.access()==null?java.util.List.of():previous.access().deviceGroupIds();
        var nextGroups=next==null || next.access()==null?java.util.List.of():next.access().deviceGroupIds();
        if((!previousTemplates.equals(nextTemplates) || !previousGroups.equals(nextGroups)) && !actor.superAdmin())
            throw new Failure(ErrorCode.FORBIDDEN,403,"Super administrator role required for legacy privileges");
        boolean privileged = (previous != null && previous.access() != null && previous.access().role() != io.ololabs.toolgate.contracts.UserRole.BASIC)
            || (next != null && next.access() != null && next.access().role() != io.ololabs.toolgate.contracts.UserRole.BASIC);
        if (privileged && !actor.superAdmin()) throw new Failure(ErrorCode.FORBIDDEN,403,"Super administrator role required");
        if (previous != null && previous.enabled() && previous.access() != null && previous.access().role() == io.ololabs.toolgate.contracts.UserRole.SUPER_ADMIN
            && (next == null || !next.enabled() || next.access() == null || next.access().role() != io.ololabs.toolgate.contracts.UserRole.SUPER_ADMIN)) {
            long remaining = before.entries().values().stream().filter(e -> e.id().kind() == Kind.USER && e.enabled() && !e.id().equals(previous == null ? null : old.id()))
                .map(e -> codec.model(e.document(),io.ololabs.toolgate.contracts.ControlUser.class))
                .filter(u -> u.access() != null && u.access().role() == io.ololabs.toolgate.contracts.UserRole.SUPER_ADMIN).count();
            if (remaining == 0) throw Failure.conflict();
        }
    }
    private void retainSuperAdmin(Directory before, Directory after) {
        java.util.function.Predicate<Directory.Entry> active = entry -> {
            if(entry.id().kind()!=Kind.USER || !entry.enabled()) return false;
            var user=codec.model(entry.document(),io.ololabs.toolgate.contracts.ControlUser.class);
            return user.access()!=null && user.access().role()==io.ololabs.toolgate.contracts.UserRole.SUPER_ADMIN;
        };
        if(before.entries().values().stream().anyMatch(active) && after.entries().values().stream().noneMatch(active)) throw Failure.conflict();
    }
    public Store.Reply get(Actor actor, Kind kind, String id) {
        return store.transaction(actor.tenant(), false, tx -> {
            var entry = tx.load().entries().get(kind.id(id));
            if (entry == null) throw new Failure(ErrorCode.NOT_FOUND, 404, "Record not found");
            return new Store.Reply(200, entry.document(), entry.revision());
        });
    }
    public Store.Reply page(Actor actor, Kind kind, String cursor, int limit) {
        if (limit < 1 || limit > 100) throw Failure.validation();
        String after = "";
        if (cursor != null) {
            try {
                if (cursor.length() > 512) throw Failure.validation();
                var parts = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8).split("\\n", -1);
                if (parts.length != 3 || !parts[0].equals(actor.tenant().value()) || !parts[1].equals(kind.name())) throw Failure.validation();
                after = Ids.valid(parts[2]);
            } catch (IllegalArgumentException e) { throw Failure.validation(); }
        }
        final String position = after;
        return store.transaction(actor.tenant(), false, tx -> {
            var directory = tx.load();
            var rows = directory.entries().values().stream().filter(e -> e.id().kind() == kind)
                .filter(e -> e.id().value().compareTo(position) > 0)
                .sorted(Comparator.comparing(e -> e.id().value())).limit(limit + 1L).toList();
            var body = new HashMap<String, Object>();
            body.put("items", rows.stream().limit(limit).map(e -> codec.value(e.document())).toList());
            if (rows.size() > limit) body.put("nextCursor", Base64.getUrlEncoder().withoutPadding().encodeToString(
                (actor.tenant().value() + "\n" + kind.name() + "\n" + rows.get(limit - 1).id().value()).getBytes(StandardCharsets.UTF_8)));
            return new Store.Reply(200, codec.json(body), directory.revision());
        });
    }
    public Store.Reply export(Actor actor, boolean yaml) {
        return store.transaction(actor.tenant(), false, tx -> {
            var directory = tx.load();
            return new Store.Reply(200, codec.snapshot(actor.tenant(), directory, yaml), directory.revision());
        });
    }
    public Store.Reply audit(Actor actor, long after, int limit) {
        actor.requireAdmin();
        if (after < 0 || limit < 1 || limit > 100) throw Failure.validation();
        return store.transaction(actor.tenant(), false, tx -> new Store.Reply(200, tx.auditPage(after, limit), 0));
    }
    public Store.Reply mutate(Actor actor, Kind kind, String id, String operation, String document,
                              long expected, String key, String requestId) {
        actor.requireAdmin();
        if(kind==Kind.ROLE && !actor.superAdmin()) throw new Failure(ErrorCode.FORBIDDEN,403,"Super administrator role required");
        if (!java.util.Set.of("CREATE", "UPDATE", "DELETE").contains(operation)) throw Failure.validation();
        Ids.valid(key); Ids.valid(requestId);
        var typedId = kind.id(id);
        var input = operation.equals("DELETE") ? null : codec.entry(kind, document);
        if (input != null && !input.id().equals(typedId)) throw Failure.validation();
        var digest = digest(operation + "\n" + kind + "\n" + id + "\n" + expected + "\n" + (input == null ? "" : input.document()));
        return store.transaction(actor.tenant(), true, tx -> {
            var before = tx.load();
            var old = before.entries().get(typedId);
            protectAssignments(actor,old,input);
            var replay = tx.replay(actor.id(), key, digest);
            if (replay != null) return replay;
            if (kind == Kind.USER) protectRole(actor,old,input,before);
            if (operation.equals("CREATE")) {
                if (old != null || tx.used(typedId) || input == null || input.revision() != 1) throw Failure.conflict();
            } else if (old == null || old.revision() != expected || (input != null && input.revision() != expected)) {
                throw Failure.conflict();
            }
            var entries = new HashMap<>(before.entries());
            if (operation.equals("DELETE")) entries.remove(typedId);
            else entries.put(typedId, codec.revision(input, old == null ? 1 : old.revision() + 1));
            Directory.Entry membership = null;
            var enrollmentPolicies = new ArrayList<Directory.Entry>();
            if (kind == Kind.USER && operation.equals("CREATE") && !defaultTeamId.isEmpty()) {
                var teamId = Kind.TEAM.id(defaultTeamId);
                var existingTeam = entries.get(teamId);
                if (existingTeam == null && tx.used(teamId)) throw Failure.conflict();
                var team = existingTeam == null ? null : codec.model(existingTeam.document(), io.ololabs.toolgate.contracts.ControlTeam.class);
                if(team!=null && team.roleIds()!=null && !team.roleIds().isEmpty() && !actor.superAdmin()) throw new Failure(ErrorCode.FORBIDDEN,403,"Super administrator role required");
                var members = new java.util.TreeSet<String>(team == null ? java.util.List.of() : team.userIds());
                members.add(id);
                membership = codec.entry(Kind.TEAM, codec.json(new io.ololabs.toolgate.contracts.ControlTeam(
                    defaultTeamId, team == null ? "Default team" : team.name(), team == null || team.enabled(),
                    team == null ? 1L : team.revision() + 1, java.util.List.copyOf(members), team == null ? null : team.deviceIds(), team == null ? null : team.roleIds())));
                entries.put(teamId, membership);
                for (var policyId : defaultPolicyIds) {
                    var existingPolicy = entries.get(Kind.POLICY.id(policyId));
                    // Bootstrap creates users before tools/policies. Missing policies confer no access.
                    if (existingPolicy == null) continue;
                    var policy = codec.model(existingPolicy.document(), io.ololabs.toolgate.contracts.ControlPolicy.class);
                    if (policy.teamIds().contains(defaultTeamId)) continue;
                    var teams = new java.util.TreeSet<>(policy.teamIds()); teams.add(defaultTeamId);
                    var assigned = codec.entry(Kind.POLICY, codec.json(new io.ololabs.toolgate.contracts.ControlPolicy(
                        policy.id(), policy.name(), policy.enabled(), policy.revision() + 1, policy.toolId(), policy.action(),
                        policy.resource(), policy.decision(), policy.userIds(), java.util.List.copyOf(teams), policy.agentIds(), policy.deviceIds())));
                    entries.put(assigned.id(), assigned); enrollmentPolicies.add(assigned);
                }
            }
            var after = validated(before.revision() + 1, entries);
            retainSuperAdmin(before,after);
            tx.save(before, after);
            tx.audit(actor.id(), operation, kind.path() + ":" + id, after.revision(), requestId, digest);
            if (membership != null) tx.audit(actor.id(), before.entries().containsKey(membership.id()) ? "UPDATE" : "CREATE", "teams:" + defaultTeamId, after.revision(), requestId,
                digest("DEFAULT_MEMBERSHIP\n" + membership.document()));
            for (var policy : enrollmentPolicies) tx.audit(actor.id(), "UPDATE", "policies:" + policy.id().value(), after.revision(), requestId,
                digest("DEFAULT_POLICY_SCOPE\n" + policy.document()));
            var reply = operation.equals("DELETE") ? new Store.Reply(204, "", after.revision())
                : new Store.Reply(old == null ? 201 : 200, after.entries().get(typedId).document(), after.entries().get(typedId).revision());
            tx.remember(actor.id(), key, digest, reply);
            return reply;
        });
    }
    public Store.Reply importConfig(Actor actor, String document, boolean yaml, long expected, String key, String requestId) {
        actor.requireAdmin(); Ids.valid(key); Ids.valid(requestId);
        var input = codec.input(document, yaml, actor.tenant());
        if (input.directory().revision() != expected) throw Failure.conflict();
        var digest = digest("IMPORT\n" + expected + "\n" + input.replace() + "\n" + input.dryRun() + "\n" + codec.snapshot(actor.tenant(), input.directory(), false));
        return store.transaction(actor.tenant(), true, tx -> {
            var replay = input.dryRun() ? null : tx.replay(actor.id(), key, digest);
            if (replay != null) return replay;
            var before = tx.load();
            if (before.revision() != expected) throw Failure.conflict();
            for(var entry:input.directory().entries().values()) protectAssignments(actor,before.entries().get(entry.id()),entry);
            if(input.replace()) for(var entry:before.entries().values()) if(!input.directory().entries().containsKey(entry.id())) protectAssignments(actor,entry,null);
            for (var entry : input.directory().entries().values()) if (entry.id().kind() == Kind.USER)
                protectRole(actor,before.entries().get(entry.id()),entry,before);
            if (input.replace()) for (var entry : before.entries().values()) if (entry.id().kind() == Kind.USER && !input.directory().entries().containsKey(entry.id()))
                protectRole(actor,entry,null,before);
            var entries = input.replace() ? new HashMap<Ids.RecordId, Directory.Entry>() : new HashMap<>(before.entries());
            for (var entry : input.directory().entries().values()) {
                var old = before.entries().get(entry.id());
                if (old == null && tx.used(entry.id())) throw Failure.conflict();
                var normalized = codec.revision(entry, old == null ? 1 : old.revision());
                entries.put(entry.id(), old != null && old.document().equals(normalized.document()) ? old
                    : codec.revision(entry, old == null ? 1 : old.revision() + 1));
            }
            var changes = new ArrayList<Map<String, String>>();
            var keys = new java.util.HashSet<>(before.entries().keySet()); keys.addAll(entries.keySet());
            keys.stream().sorted(Comparator.comparing((Ids.RecordId i) -> i.kind().name()).thenComparing(Ids.RecordId::value)).forEach(id -> {
                var old = before.entries().get(id); var now = entries.get(id);
                if (!java.util.Objects.equals(old, now)) changes.add(Map.of("kind", id.kind().name(), "id", id.value(),
                    "operation", old == null ? "CREATE" : now == null ? "DELETE" : "UPDATE"));
            });
            var after = validated(before.revision() + (changes.isEmpty() || input.dryRun() ? 0 : 1), entries);
            retainSuperAdmin(before,after);
            var reply = new Store.Reply(200, codec.result(!input.dryRun(), after.revision(), changes), after.revision());
            if (!input.dryRun()) {
                tx.save(before, after);
                // Even an applied no-op import has a durable administrative audit record.
                tx.audit(actor.id(), "IMPORT", "config", after.revision(), requestId, digest);
                tx.remember(actor.id(), key, digest, reply);
            }
            return reply;
        });
    }
    private Directory validated(long revision, Map<Ids.RecordId, Directory.Entry> entries) {
        try {
            var directory = new Directory(revision, entries);
            directory.validate(maxRecords, maxBytes); codec.validatePolicies(directory); return directory;
        } catch (IllegalArgumentException e) { throw Failure.conflict(); }
    }
    public static String digest(String value) {
        return digest(value.getBytes(StandardCharsets.UTF_8));
    }
    public static String digest(byte[] value) {
        try { return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value)); }
        catch (java.security.NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
}
