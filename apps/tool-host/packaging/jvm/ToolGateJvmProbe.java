// Copyright 2026 OLO Labs; SPDX-License-Identifier: Apache-2.0
import java.nio.file.Files;
import java.nio.file.Path;
import java.net.ServerSocket;
import java.util.LinkedHashMap;
import java.util.Map;

/** Executed by the actual JVM under the worker policy before tier registration. */
public final class ToolGateJvmProbe {
    private static String read(String path) throws Exception {
        return Files.readString(Path.of(path)).strip();
    }
    private static String quote(String value) {
        var out = new StringBuilder("\"");
        for (char c : value.toCharArray()) {
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                default -> {
                    if (c < 32) out.append(String.format("\\u%04x", (int)c));
                    else out.append(c);
                }
            }
        }
        return out.append('"').toString();
    }
    private static long statusNumber(String status, String field) {
        return status.lines().filter(s -> s.startsWith(field + ":"))
            .map(s -> s.substring(s.indexOf(':') + 1).strip().split("\\s+")[0])
            .mapToLong(Long::parseLong).findFirst().orElseThrow();
    }
    public static void main(String[] args) throws Exception {
        if (Runtime.version().feature() != 21) throw new IllegalStateException("Java 21 required");
        String status = read("/proc/self/status");
        var fields = new LinkedHashMap<String, Object>();
        fields.put("uid", statusNumber(status, "Uid"));
        fields.put("gid", statusNumber(status, "Gid"));
        fields.put("pid", ProcessHandle.current().pid());
        fields.put("status", status);
        fields.put("uidMap", read("/proc/self/uid_map"));
        fields.put("gidMap", read("/proc/self/gid_map"));
        fields.put("netns", Files.readSymbolicLink(Path.of("/proc/self/ns/net")).toString());
        fields.put("userns", Files.readSymbolicLink(Path.of("/proc/self/ns/user")).toString());
        boolean denied = false;
        try (var socket = new ServerSocket(0)) {
            // A usable socket is a qualification failure, regardless of network namespace.
        } catch (java.net.SocketException failure) {
            // Match Linux EPERM, not unrelated library, address or resource failures.
            denied = failure.getMessage() != null && failure.getMessage().contains("Operation not permitted");
        }
        fields.put("networkDenied", denied);
        for (var entry : Map.of("memory", "memory.max", "swap", "memory.swap.max",
                "pids", "pids.max", "cpu", "cpu.max", "io", "io.max").entrySet()) {
            fields.put(entry.getKey(), read("/sys/fs/cgroup/" + entry.getValue()));
        }
        var json = new StringBuilder("{");
        fields.forEach((key, value) -> {
            if (json.length() > 1) json.append(',');
            json.append(quote(key)).append(':').append(value instanceof String s ? quote(s) : value);
        });
        System.out.println(json.append('}'));
        System.out.flush();
        System.in.read(); // Hold live namespaces until paired cleanup completes.
    }
}
