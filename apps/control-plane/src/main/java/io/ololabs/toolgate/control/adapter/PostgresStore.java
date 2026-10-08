// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
package io.ololabs.toolgate.control.adapter;

import io.agroal.api.AgroalDataSource;
import io.ololabs.toolgate.control.application.Store;
import io.ololabs.toolgate.control.application.Failure;
import io.ololabs.toolgate.control.application.McpStore;
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
    public PostgresStore(jakarta.enterprise.inject.Instance<AgroalDataSource> dataSource, ContractCodec codec, org.eclipse.microprofile.config.Config config) {
        this(config.getOptionalValue("toolgate.quickstart.enabled", Boolean.class).orElse(false)
            && !config.getOptionalValue("toolgate.quickstart.storage", String.class).orElse("sqlite").equals("postgresql")
            ? SqliteState.open(java.nio.file.Path.of(config.getValue("toolgate.quickstart.database", String.class))) : (javax.sql.DataSource) dataSource.get(), codec);
    }
    public PostgresStore(javax.sql.DataSource dataSource, ContractCodec codec) { this.dataSource = dataSource; this.codec = codec; }
    public <T> T transaction(TenantId tenant, boolean write, Function<Session, T> work) {
        try (var connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            boolean local = SqliteState.local(connection);
            if (!local) {
                connection.setTransactionIsolation(write ? Connection.TRANSACTION_READ_COMMITTED : Connection.TRANSACTION_REPEATABLE_READ);
                connection.setReadOnly(!write);
            }
            try {
                if (!local) try (var statement = connection.createStatement()) {
                    statement.execute("SET LOCAL statement_timeout = '5s'"); statement.execute("SET LOCAL lock_timeout = '3s'");
                }
                if (write && !local) try (var statement = connection.prepareStatement("SELECT pg_advisory_xact_lock(hashtextextended(?, 0))")) {
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
        public io.ololabs.toolgate.control.application.FleetStore fleet() { return new PostgresFleetStore(connection, tenant); }
        public io.ololabs.toolgate.control.application.BuilderStore builder() { return new PostgresBuilderStore(connection, tenant); }
        private EnrollmentRecord enrollmentRows(java.sql.PreparedStatement statement) throws SQLException {
            try (statement; var rows=statement.executeQuery()) {
                return rows.next() ? new EnrollmentRecord(rows.getString(1),rows.getString(2),rows.getString(3),rows.getString(4),
                    rows.getString(5),rows.getString(6),rows.getString(7),rows.getLong(8),rows.getLong(9)) : null;
            }
        }
        public EnrollmentRecord enrollment(String id) {
            try { return enrollmentRows(statement("SELECT enrollment_id,code_digest,device_digest,document,csr,user_id,certificate,expires_at,last_poll FROM control_enrollments WHERE tenant_id=? AND enrollment_id=?",id)); }
            catch (SQLException e) { throw Failure.unavailable(); }
        }
        public EnrollmentRecord enrollmentCode(String digest) {
            try { return enrollmentRows(statement("SELECT enrollment_id,code_digest,device_digest,document,csr,user_id,certificate,expires_at,last_poll FROM control_enrollments WHERE tenant_id=? AND code_digest=?",digest)); }
            catch (SQLException e) { throw Failure.unavailable(); }
        }
        public void saveEnrollment(EnrollmentRecord e) {
            try (var s=statement("INSERT INTO control_enrollments (tenant_id,enrollment_id,code_digest,device_digest,document,csr,user_id,certificate,expires_at,last_poll) VALUES (?,?,?,?,?,?,?,?,?,?) ON CONFLICT (tenant_id,enrollment_id) DO UPDATE SET document=excluded.document,user_id=excluded.user_id,certificate=excluded.certificate,last_poll=excluded.last_poll",
                e.id(),e.codeDigest(),e.deviceDigest(),e.document(),e.csr(),e.userId(),e.certificate(),e.expiresAt(),e.lastPoll())) { s.executeUpdate(); }
            catch (SQLException failure) { throw Failure.unavailable(); }
        }
        public long pendingEnrollments(long now) {
            try (var s=statement("SELECT count(*) FROM control_enrollments WHERE tenant_id=? AND expires_at>?",now);var rows=s.executeQuery()) {rows.next();return rows.getLong(1);}
            catch (SQLException e) { throw Failure.unavailable(); }
        }
        public java.util.List<EnrollmentRecord> enrollments(long now) {
            try (var s=statement("SELECT enrollment_id,code_digest,device_digest,document,csr,user_id,certificate,expires_at,last_poll FROM control_enrollments WHERE tenant_id=? AND expires_at>? ORDER BY expires_at,enrollment_id LIMIT 32",now);var rows=s.executeQuery()) {
                var result=new java.util.ArrayList<EnrollmentRecord>();
                while(rows.next())result.add(new EnrollmentRecord(rows.getString(1),rows.getString(2),rows.getString(3),rows.getString(4),rows.getString(5),rows.getString(6),rows.getString(7),rows.getLong(8),rows.getLong(9)));
                return java.util.List.copyOf(result);
            }catch(SQLException failure){throw Failure.unavailable();}
        }
        public void pruneEnrollments(long now) {
            try (var s=statement("DELETE FROM control_enrollments WHERE tenant_id=? AND expires_at<=?",now)) {s.executeUpdate();}
            catch (SQLException e) { throw Failure.unavailable(); }
        }
        private EndpointRecord endpointRows(java.sql.PreparedStatement statement) throws SQLException {
            try (statement;var rows=statement.executeQuery()) {return rows.next()?new EndpointRecord(rows.getString(1),rows.getString(2),rows.getString(3),rows.getString(4),rows.getString(5),rows.getString(6)):null;}
        }
        public EndpointRecord endpoint(String id) {
            try {return endpointRows(statement("SELECT device_id,key_fingerprint,document,csr,report_digest,acknowledgment FROM control_endpoints WHERE tenant_id=? AND device_id=?",id));}
            catch (SQLException e) { throw Failure.unavailable(); }
        }
        public EndpointRecord endpointKey(String fingerprint) {
            try {return endpointRows(statement("SELECT device_id,key_fingerprint,document,csr,report_digest,acknowledgment FROM control_endpoints WHERE tenant_id=? AND key_fingerprint=?",fingerprint));}
            catch (SQLException e) { throw Failure.unavailable(); }
        }
        public void saveEndpoint(EndpointRecord e) {
            try (var s=statement("INSERT INTO control_endpoints (tenant_id,device_id,key_fingerprint,document,csr,report_digest,acknowledgment) VALUES (?,?,?,?,?,?,?) ON CONFLICT (tenant_id,device_id) DO UPDATE SET document=excluded.document,report_digest=excluded.report_digest,acknowledgment=excluded.acknowledgment",
                e.id(),e.fingerprint(),e.document(),e.csr(),e.reportDigest(),e.acknowledgment())) {s.executeUpdate();}
            catch (SQLException failure) {throw Failure.unavailable();}
        }
        public EndpointConfiguration endpointConfiguration(String deviceId) {
            try (var s=statement("SELECT source_revision,document,acknowledged_digest,local_tools FROM control_endpoint_configurations WHERE tenant_id=? AND device_id=?",deviceId);var rows=s.executeQuery()) {
                return rows.next()?new EndpointConfiguration(deviceId,rows.getLong(1),rows.getString(2),rows.getString(3),rows.getString(4)):null;
            } catch(SQLException failure){throw Failure.unavailable();}
        }
        public void saveEndpointConfiguration(EndpointConfiguration configuration) {
            try(var s=statement("INSERT INTO control_endpoint_configurations (tenant_id,device_id,source_revision,document,acknowledged_digest,local_tools) VALUES (?,?,?,?,?,?) ON CONFLICT (tenant_id,device_id) DO UPDATE SET source_revision=excluded.source_revision,document=excluded.document,acknowledged_digest=excluded.acknowledged_digest,local_tools=excluded.local_tools",
                configuration.deviceId(),configuration.sourceRevision(),configuration.document(),configuration.acknowledgedDigest(),configuration.localTools())) {
                if(s.executeUpdate()!=1)throw Failure.conflict();
            } catch(SQLException failure){throw Failure.unavailable();}
        }
        public McpStore mcp() {
            return new McpStore() {
                private Row row(java.sql.ResultSet rows)throws SQLException{return new Row(rows.getString(1),rows.getString(2),rows.getString(3),rows.getLong(4),rows.getString(5),rows.getString(6),rows.getString(7));}
                private java.util.List<Row> query(String suffix,Object... parameters){
                    try(var s=statement("SELECT request_id,device_id,state,expires_at,record,task,result FROM control_mcp_requests WHERE tenant_id=? "+suffix,parameters);var rows=s.executeQuery()){
                        var values=new java.util.ArrayList<Row>();while(rows.next())values.add(row(rows));return java.util.List.copyOf(values);
                    }catch(SQLException failure){throw Failure.unavailable();}
                }
                public Row get(String id){var rows=query("AND request_id=?",id);return rows.isEmpty()?null:rows.getFirst();}
                public java.util.List<Row> page(int limit){return query("ORDER BY received_at DESC,request_id LIMIT ?",limit);}
                public java.util.List<Row> pending(String deviceId,int limit){return query("AND device_id=? AND state IN ('WAITING_FOR_POLL','SUBMITTED') ORDER BY received_at,request_id LIMIT ?",deviceId,limit);}
                public void save(Row row){try(var s=statement("INSERT INTO control_mcp_requests (tenant_id,request_id,device_id,state,expires_at,received_at,record,task,result) VALUES (?,?,?,?,?,?,?,?,?) ON CONFLICT (tenant_id,request_id) DO UPDATE SET state=excluded.state,record=excluded.record,result=excluded.result",
                    row.id(),row.deviceId(),row.state(),row.expiresAt(),codec.model(row.record(),io.ololabs.toolgate.contracts.RemoteToolRecord.class).receivedAtUnixMs(),row.record(),row.task(),row.result())){if(s.executeUpdate()!=1)throw Failure.conflict();}catch(SQLException failure){throw Failure.unavailable();}}
                public void prune(long before){try(var s=statement("DELETE FROM control_mcp_requests WHERE tenant_id=? AND expires_at<?",before)){s.executeUpdate();}catch(SQLException failure){throw Failure.unavailable();}}
                public long count(){try(var s=statement("SELECT count(*) FROM control_mcp_requests WHERE tenant_id=?");var rows=s.executeQuery()){rows.next();return rows.getLong(1);}catch(SQLException failure){throw Failure.unavailable();}}
            };
        }
        public long bundleSequence() {
            try (var statement = statement("SELECT COALESCE(max(sequence),0) FROM control_policy_bundles WHERE tenant_id=?"); var rows = statement.executeQuery()) {
                rows.next(); return rows.getLong(1);
            } catch (SQLException e) { throw Failure.unavailable(); }
        }
        public BundleRecord bundle(long sequence) {
            try (var statement = statement("SELECT document,policy,directory_revision FROM control_policy_bundles WHERE tenant_id=? AND sequence=?", sequence); var rows = statement.executeQuery()) {
                return rows.next() ? new BundleRecord(sequence, rows.getString(1), rows.getString(2), rows.getLong(3)) : null;
            } catch (SQLException e) { throw Failure.unavailable(); }
        }
        public void publishBundle(BundleRecord bundle) {
            try (var statement = statement("INSERT INTO control_policy_bundles (tenant_id,sequence,directory_revision,document,policy) VALUES (?,?,?,?,?)",
                    bundle.sequence(), bundle.directoryRevision(), bundle.document(), bundle.policy())) {
                statement.executeUpdate();
            } catch (SQLException e) { throw Failure.unavailable(); }
        }
        public ApprovalRecord approval(String id) {
            try (var statement = statement("SELECT approval_id,binding_digest,document,expires_at FROM control_approvals WHERE tenant_id=? AND approval_id=?", id);
                 var rows = statement.executeQuery()) {
                return rows.next() ? approvalRow(rows) : null;
            } catch (SQLException e) { throw Failure.unavailable(); }
        }
        public ApprovalRecord approvalBinding(String digest, long now) {
            try (var statement = statement("SELECT approval_id,binding_digest,document,expires_at FROM control_approvals WHERE tenant_id=? AND binding_digest=? AND expires_at>? ORDER BY approval_id DESC LIMIT 1", digest, now);
                 var rows = statement.executeQuery()) {
                return rows.next() ? approvalRow(rows) : null;
            } catch (SQLException e) { throw Failure.unavailable(); }
        }
        private ApprovalRecord approvalRow(java.sql.ResultSet rows) throws SQLException {
            return new ApprovalRecord(rows.getString(1), rows.getString(2), rows.getString(3), rows.getLong(4));
        }
        public java.util.List<ApprovalRecord> approvals(String after, int limit) {
            try (var statement = statement("SELECT approval_id,binding_digest,document,expires_at FROM control_approvals WHERE tenant_id=? AND approval_id>? ORDER BY approval_id LIMIT ?", after, limit);
                 var rows = statement.executeQuery()) {
                var result = new java.util.ArrayList<ApprovalRecord>();
                while (rows.next()) result.add(approvalRow(rows)); return java.util.List.copyOf(result);
            } catch (SQLException e) { throw Failure.unavailable(); }
        }
        public long activeApprovals(long now) {
            try (var statement = statement("SELECT count(*) FROM control_approvals WHERE tenant_id=? AND expires_at>?", now); var rows = statement.executeQuery()) {
                rows.next(); return rows.getLong(1);
            } catch (SQLException e) { throw Failure.unavailable(); }
        }
        public void saveApproval(ApprovalRecord approval) {
            try (var statement = statement("INSERT INTO control_approvals (tenant_id,approval_id,binding_digest,document,expires_at) VALUES (?,?,?,?,?) ON CONFLICT (tenant_id,approval_id) DO UPDATE SET document=excluded.document,expires_at=excluded.expires_at WHERE control_approvals.binding_digest=excluded.binding_digest AND (control_approvals.document::jsonb->>'revision')::bigint < (excluded.document::jsonb->>'revision')::bigint",
                approval.id(), approval.bindingDigest(), approval.document(), approval.expiresAt())) {
                if (statement.executeUpdate()!=1) throw Failure.conflict();
            }
            catch (SQLException e) { throw Failure.unavailable(); }
        }
        public PermitLease permit(String jti) {
            try (var statement = statement("SELECT approval_id,binding_digest,request_id,issued_at,expires_at,consumed_at FROM control_permit_leases WHERE tenant_id=? AND jti=?", jti); var rows = statement.executeQuery()) {
                if (!rows.next()) return null;
                var consumed = rows.getLong(6); Long consumedAt = rows.wasNull() ? null : consumed;
                return new PermitLease(jti, rows.getString(1), rows.getString(2), rows.getString(3), rows.getLong(4), rows.getLong(5), consumedAt);
            } catch (SQLException e) { throw Failure.unavailable(); }
        }
        public void lease(PermitLease permit) {
            try (var statement = statement("INSERT INTO control_permit_leases (tenant_id,jti,approval_id,binding_digest,request_id,issued_at,expires_at) VALUES (?,?,?,?,?,?,?)",
                permit.jti(), permit.approvalId(), permit.bindingDigest(), permit.requestId(), permit.issuedAt(), permit.expiresAt())) { statement.executeUpdate(); }
            catch (SQLException e) { throw Failure.unavailable(); }
        }
        public void consumePermit(String jti, long consumedAt) {
            try (var statement = connection.prepareStatement("UPDATE control_permit_leases SET consumed_at=? WHERE tenant_id=? AND jti=? AND consumed_at IS NULL")) {
                statement.setLong(1, consumedAt); statement.setString(2, tenant); statement.setString(3, jti);
                if (statement.executeUpdate() != 1) throw Failure.conflict();
            } catch (SQLException e) { throw Failure.unavailable(); }
        }
        public long approvalClock(long now) {
            try (var statement = statement("INSERT INTO control_approval_clocks (tenant_id,observed_at) VALUES (?,?) ON CONFLICT (tenant_id) DO UPDATE SET observed_at=GREATEST(control_approval_clocks.observed_at,excluded.observed_at) RETURNING observed_at", now);
                 var rows = statement.executeQuery()) { rows.next(); return rows.getLong(1); }
            catch (SQLException e) { throw Failure.unavailable(); }
        }
        public long activePermits(long now) {
            try (var statement = statement("SELECT count(*) FROM control_permit_leases WHERE tenant_id=? AND expires_at>?", now); var rows = statement.executeQuery()) {
                rows.next(); return rows.getLong(1);
            } catch (SQLException e) { throw Failure.unavailable(); }
        }
        private java.sql.PreparedStatement statement(String sql, Object... params) throws SQLException {
            var statement = connection.prepareStatement(SqliteState.sql(connection, sql)); statement.setString(1, tenant);
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
                        "occurredAt", SqliteState.local(connection) ? rows.getString(8) : rows.getObject(8, java.time.OffsetDateTime.class).toInstant().toString()));
                }
                return codec.json(more ? Map.of("items", items, "nextCursor", Long.toString(sequence)) : Map.of("items", items));
            } catch (SQLException e) { throw Failure.unavailable(); }
        }
    }
}
