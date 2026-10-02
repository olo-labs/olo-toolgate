// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
package io.ololabs.toolgate.control.domain;

/** Typed directory identifiers; tenant identity always comes from verified authentication. */
public final class Ids {
    private Ids() {}
    public static String valid(String value) {
        if (value == null || !value.matches("[a-zA-Z0-9][a-zA-Z0-9._:/-]{0,127}")) {
            throw new IllegalArgumentException("Invalid identifier");
        }
        return value;
    }
    public record TenantId(String value) { public TenantId { valid(value); } }
    public sealed interface RecordId permits UserId, TeamId, AgentId, ToolId, PolicyId, DeviceId {
        String value();
        Kind kind();
    }
    public record UserId(String value) implements RecordId {
        public UserId { valid(value); } public Kind kind() { return Kind.USER; }
    }
    public record TeamId(String value) implements RecordId {
        public TeamId { valid(value); } public Kind kind() { return Kind.TEAM; }
    }
    public record AgentId(String value) implements RecordId {
        public AgentId { valid(value); } public Kind kind() { return Kind.AGENT; }
    }
    public record ToolId(String value) implements RecordId {
        public ToolId { valid(value); } public Kind kind() { return Kind.TOOL; }
    }
    public record PolicyId(String value) implements RecordId {
        public PolicyId { valid(value); } public Kind kind() { return Kind.POLICY; }
    }
    public record DeviceId(String value) implements RecordId {
        public DeviceId { valid(value); } public Kind kind() { return Kind.DEVICE; }
    }
    public enum Kind {
        USER("users", "ControlUser"), TEAM("teams", "ControlTeam"),
        AGENT("agents", "ControlAgent"), TOOL("tools", "ControlTool"),
        POLICY("policies", "ControlPolicy"), DEVICE("devices", "ControlDevice");
        private final String path;
        private final String model;
        Kind(String path, String model) { this.path = path; this.model = model; }
        public String path() { return path; }
        public String model() { return model; }
        public RecordId id(String value) {
            return switch (this) {
                case USER -> new UserId(value); case TEAM -> new TeamId(value);
                case AGENT -> new AgentId(value); case TOOL -> new ToolId(value);
                case POLICY -> new PolicyId(value); case DEVICE -> new DeviceId(value);
            };
        }
        public static Kind path(String value) {
            for (var kind : values()) if (kind.path.equals(value)) return kind;
            throw new IllegalArgumentException("Unknown record kind");
        }
    }
}
