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
    public DirectoryService(Store store, Codec codec, int maxRecords, int maxBytes) {
        this.store = store; this.codec = codec; this.maxRecords = maxRecords; this.maxBytes = maxBytes;
    }
    public record Actor(TenantId tenant, String id, boolean admin) {
        public Actor {
            java.util.Objects.requireNonNull(tenant);
            if (id == null || !id.matches("[a-f0-9]{64}")) throw Failure.validation();
        }
        public void requireAdmin() {
            if (!admin) throw new Failure(ErrorCode.FORBIDDEN, 403, "Administrator role required");
        }
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
        if (!java.util.Set.of("CREATE", "UPDATE", "DELETE").contains(operation)) throw Failure.validation();
        Ids.valid(key); Ids.valid(requestId);
        var typedId = kind.id(id);
        var input = operation.equals("DELETE") ? null : codec.entry(kind, document);
        if (input != null && !input.id().equals(typedId)) throw Failure.validation();
        var digest = digest(operation + "\n" + kind + "\n" + id + "\n" + expected + "\n" + (input == null ? "" : input.document()));
        return store.transaction(actor.tenant(), true, tx -> {
            var replay = tx.replay(actor.id(), key, digest);
            if (replay != null) return replay;
            var before = tx.load();
            var old = before.entries().get(typedId);
            if (operation.equals("CREATE")) {
                if (old != null || tx.used(typedId) || input == null || input.revision() != 1) throw Failure.conflict();
            } else if (old == null || old.revision() != expected || (input != null && input.revision() != expected)) {
                throw Failure.conflict();
            }
            var entries = new HashMap<>(before.entries());
            if (operation.equals("DELETE")) entries.remove(typedId);
            else entries.put(typedId, codec.revision(input, old == null ? 1 : old.revision() + 1));
            var after = validated(before.revision() + 1, entries);
            tx.save(before, after);
            tx.audit(actor.id(), operation, kind.path() + ":" + id, after.revision(), requestId, digest);
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
