// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
package io.ololabs.toolgate.control.adapter;

import io.agroal.api.AgroalDataSource;
import io.ololabs.toolgate.control.application.Store;
import io.ololabs.toolgate.control.application.Failure;
import io.ololabs.toolgate.control.domain.Directory;
import io.ololabs.toolgate.control.domain.Ids.Kind;
import io.ololabs.toolgate.control.domain.Ids.RecordId;
import io.ololabs.toolgate.control.domain.Ids.TenantId;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/** JDBC adapter with database-backed tenant serialization and atomic audit/replay storage. */
@ApplicationScoped
public class PostgresStore implements Store {
    private final javax.sql.DataSource dataSource;
    private final ContractCodec codec;
    @Inject
    public PostgresStore(AgroalDataSource dataSource, ContractCodec codec) { this((javax.sql.DataSource) dataSource, codec); }
    public PostgresStore(javax.sql.DataSource dataSource, ContractCodec codec) { this.dataSource = dataSource; this.codec = codec; }
    public <T> T transaction(TenantId tenant, boolean write, Function<Session, T> work) {
        try (var connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            connection.setTransactionIsolation(write ? Connection.TRANSACTION_READ_COMMITTED : Connection.TRANSACTION_REPEATABLE_READ);
            connection.setReadOnly(!write);
            try {
                try (var statement = connection.createStatement()) {
                    statement.execute("SET LOCAL statement_timeout = '5s'"); statement.execute("SET LOCAL lock_timeout = '3s'");
                }
                if (write) try (var statement = connection.prepareStatement("SELECT pg_advisory_xact_lock(hashtextextended(?, 0))")) {
                    statement.setString(1, tenant.value()); statement.execute();
                }
                var result = work.apply(new JdbcSession(connection, tenant.value())); connection.commit(); return result;
            } catch (RuntimeException | SQLException e) {
                connection.rollback();
                if (e instanceof RuntimeException failure) throw failure;
                throw Failure.unavailable();
            }
        } catch (SQLException e) { throw Failure.unavailable(); }
    }
    private final class JdbcSession implements Session {
        private final Connection connection;
        private final String tenant;
        JdbcSession(Connection connection, String tenant) { this.connection = connection; this.tenant = tenant; }
        private java.sql.PreparedStatement statement(String sql, Object... params) throws SQLException {
            var statement = connection.prepareStatement(sql); statement.setString(1, tenant);
            for (int i = 0; i < params.length; i++) statement.setObject(i + 2, params[i]); return statement;
        }
        public Directory load() {
            try {
                long revision = 0;
                try (var statement = statement("SELECT revision FROM control_tenants WHERE tenant_id=?"); var rows = statement.executeQuery()) {
                    if (rows.next()) revision = rows.getLong(1);
                }
                var entries = new HashMap<RecordId, Directory.Entry>();
                try (var statement = statement("SELECT kind,document::text FROM control_records WHERE tenant_id=?"); var rows = statement.executeQuery()) {
                    while (rows.next()) {
                        var entry = codec.entry(Kind.valueOf(rows.getString(1)), rows.getString(2)); entries.put(entry.id(), entry);
                    }
                }
                return new Directory(revision, entries);
            } catch (SQLException e) { throw Failure.unavailable(); }
        }
        public void save(Directory before, Directory after) {
            try {
                try (var statement = statement("INSERT INTO control_tenants (tenant_id,revision) VALUES (?,?) ON CONFLICT (tenant_id) DO UPDATE SET revision=excluded.revision", after.revision())) {
                    statement.executeUpdate();
                }
                for (var id : before.entries().keySet()) if (!after.entries().containsKey(id)) {
                    try (var statement = statement("DELETE FROM control_records WHERE tenant_id=? AND kind=? AND record_id=?", id.kind().name(), id.value())) { statement.executeUpdate(); }
                }
                for (var entry : after.entries().values()) if (!entry.equals(before.entries().get(entry.id()))) {
                    try (var statement = statement("INSERT INTO control_record_ids (tenant_id,kind,record_id) VALUES (?,?,?) ON CONFLICT DO NOTHING", entry.id().kind().name(), entry.id().value())) { statement.executeUpdate(); }
                    try (var statement = statement("INSERT INTO control_records (tenant_id,kind,record_id,revision,document) VALUES (?,?,?,?,?::jsonb) ON CONFLICT (tenant_id,kind,record_id) DO UPDATE SET revision=excluded.revision,document=excluded.document",
                            entry.id().kind().name(), entry.id().value(), entry.revision(), entry.document())) { statement.executeUpdate(); }
                }
            } catch (SQLException e) { throw Failure.unavailable(); }
        }
        public boolean used(RecordId id) {
            try (var statement = statement("SELECT 1 FROM control_record_ids WHERE tenant_id=? AND kind=? AND record_id=?", id.kind().name(), id.value()); var rows = statement.executeQuery()) {
                return rows.next();
            } catch (SQLException e) { throw Failure.unavailable(); }
        }
        public Reply replay(String actor, String key, String digest) {
            try {
                try (var statement = statement("DELETE FROM control_idempotency WHERE tenant_id=? AND expires_at <= clock_timestamp()")) { statement.executeUpdate(); }
                try (var statement = statement("SELECT request_digest,status,body,revision FROM control_idempotency WHERE tenant_id=? AND actor_id=? AND key=?", actor, key); var rows = statement.executeQuery()) {
                    if (!rows.next()) return null;
                    if (!rows.getString(1).equals(digest)) throw Failure.conflict();
                    return new Reply(rows.getInt(2), rows.getString(3), rows.getLong(4));
                }
            } catch (SQLException e) { throw Failure.unavailable(); }
        }
        public void remember(String actor, String key, String digest, Reply reply) {
            try {
                try (var statement = statement("SELECT count(*) FROM control_idempotency WHERE tenant_id=?"); var rows = statement.executeQuery()) {
                    rows.next(); if (rows.getLong(1) >= 10000) throw Failure.conflict();
                }
                try (var statement = statement("INSERT INTO control_idempotency (tenant_id,actor_id,key,request_digest,status,body,revision) VALUES (?,?,?,?,?,?,?)",
                    actor, key, digest, reply.status(), reply.body(), reply.revision())) { statement.executeUpdate(); }
            } catch (SQLException e) { throw Failure.unavailable(); }
        }
        public void audit(String actor, String operation, String target, long revision, String requestId, String digest) {
            try (var statement = statement("INSERT INTO control_audit (tenant_id,actor_id,operation,target,revision,request_id,request_digest) VALUES (?,?,?,?,?,?,?)",
                actor, operation, target, revision, requestId, digest)) { statement.executeUpdate(); }
            catch (SQLException e) { throw Failure.unavailable(); }
        }
        public String auditPage(long after, int limit) {
            try (var statement = statement("SELECT sequence,actor_id,operation,target,revision,request_id,request_digest,occurred_at FROM control_audit WHERE tenant_id=? AND sequence>? ORDER BY sequence LIMIT ?", after, limit + 1);
                 var rows = statement.executeQuery()) {
                var items = new java.util.ArrayList<Object>(); long sequence = after; boolean more = false;
                while (rows.next()) {
                    if (items.size() == limit) { more = true; break; }
                    sequence = rows.getLong(1);
                    items.add(Map.of("sequence", sequence, "tenantId", tenant, "actorId", rows.getString(2), "operation", rows.getString(3),
                        "target", rows.getString(4), "revision", rows.getLong(5), "requestId", rows.getString(6), "requestDigest", rows.getString(7),
                        "occurredAt", rows.getObject(8, java.time.OffsetDateTime.class).toInstant().toString()));
                }
                return codec.json(more ? Map.of("items", items, "nextCursor", Long.toString(sequence)) : Map.of("items", items));
            } catch (SQLException e) { throw Failure.unavailable(); }
        }
    }
}
