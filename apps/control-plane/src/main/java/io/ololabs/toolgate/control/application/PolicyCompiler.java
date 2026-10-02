// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
package io.ololabs.toolgate.control.application;

import io.ololabs.toolgate.control.domain.Directory;
import io.ololabs.toolgate.control.domain.Ids;
import io.ololabs.toolgate.control.domain.Ids.Kind;
import io.ololabs.toolgate.contracts.*;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

/** Deterministic, bounded compilation. No execution, credentials or framework state. */
public final class PolicyCompiler {
    private final Codec codec;
    public PolicyCompiler(Codec codec) { this.codec = codec; }
    public String compile(Directory directory) {
        return compile(directory, List.of());
    }
    /** Grace classifications are explicit administrator assertions, never guessed from names. */
    public String compile(Directory directory, List<String> gracePolicyIds) {
        var grace = new TreeSet<>(gracePolicyIds);
        for (var id : grace) {
            var selected = codec.model(required(directory, Kind.POLICY, id).document(), ControlPolicy.class);
            if (selected.decision() != Decision.ALLOW) throw Failure.validation();
        }
        directory.validate(512, 1048576); codec.validatePolicies(directory);
        var rules = new ArrayList<BundleRule>();
        directory.entries().values().stream().filter(e -> e.enabled() && e.id().kind() == Kind.POLICY)
            .sorted(java.util.Comparator.comparing(e -> e.id().value())).forEach(entry -> {
                var policy = codec.model(entry.document(), ControlPolicy.class);
                var users = new TreeSet<>(policy.userIds());
                for (var teamId : policy.teamIds()) {
                    var team = codec.model(required(directory, Kind.TEAM, teamId).document(), ControlTeam.class);
                    users.addAll(team.userIds());
                }
                // A selected empty team must never broaden into an unrestricted dimension.
                if ((!policy.userIds().isEmpty() || !policy.teamIds().isEmpty()) && users.isEmpty()) return;
                users.forEach(id -> required(directory, Kind.USER, id));
                policy.agentIds().forEach(id -> required(directory, Kind.AGENT, id));
                policy.deviceIds().forEach(id -> required(directory, Kind.DEVICE, id));
                required(directory, Kind.TOOL, policy.toolId());
                rules.add(new BundleRule(policy.id(), List.copyOf(users), sorted(policy.agentIds()), sorted(policy.deviceIds()),
                    policy.toolId(), policy.action(), policy.resource(), grace.contains(policy.id()), BundleEffect.valueOf(policy.decision().name())));
            });
        var document = codec.json(new CompiledPolicy(1L, rules));
        if (document.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 786432) throw Failure.validation();
        codec.model(document, CompiledPolicy.class); return document;
    }
    private Directory.Entry required(Directory directory, Kind kind, String id) {
        var entry = directory.entries().get(kind.id(id));
        if (entry == null || !entry.enabled()) throw Failure.validation();
        return entry;
    }
    private List<String> sorted(List<String> values) { return List.copyOf(new TreeSet<>(values)); }
}
