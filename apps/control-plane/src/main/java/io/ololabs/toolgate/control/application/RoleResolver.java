// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
package io.ololabs.toolgate.control.application;

import io.ololabs.toolgate.control.domain.Directory;
import io.ololabs.toolgate.control.domain.Ids.Kind;
import io.ololabs.toolgate.contracts.*;
import java.util.*;

/** Resolve enabled direct and team-inherited roles. Fixed templates never bypass policy. */
public final class RoleResolver {
    private final Codec codec;
    public RoleResolver(Codec codec) { this.codec=codec; }
    public record Access(UserRole portalRole, boolean managed, List<RoleRules> grants) {
        public boolean approver() { return grants.stream().anyMatch(r->r.templateIds().contains(UserPrivilegeTemplate.APPROVER)); }
    }
    public Access resolve(Directory directory, ControlUser user) {
        var access=user.access();
        var ids=new TreeSet<String>();
        if(access!=null && access.roleIds()!=null) ids.addAll(access.roleIds());
        for(var entry:directory.entries().values()) if(entry.id().kind()==Kind.TEAM && entry.enabled()) {
            var team=codec.model(entry.document(),ControlTeam.class);
            if(team.userIds().contains(user.id()) && team.roleIds()!=null) ids.addAll(team.roleIds());
        }
        var portal=access==null?UserRole.BASIC:access.role();
        var grants=new ArrayList<RoleRules>();
        for(var id:ids) {
            var entry=directory.entries().get(Kind.ROLE.id(id));
            if(entry==null || !entry.enabled()) continue;
            var role=codec.model(entry.document(),ControlRole.class);
            if(role.portalRole().ordinal()>portal.ordinal()) portal=role.portalRole();
            grants.add(role.rules());
        }
        if(ids.isEmpty() && access!=null) {
            boolean scoped=access.templateIds().contains(UserPrivilegeTemplate.IT_CLOUD_ADMIN)||!access.deviceGroupIds().isEmpty();
            grants.add(new RoleRules(access.templateIds(),scoped?RoleDeviceScope.GROUPS:RoleDeviceScope.ALL,access.deviceGroupIds(),List.of()));
        }
        return new Access(portal,!ids.isEmpty(),List.copyOf(grants));
    }
    /** null means policy's original device scope; an empty set means no runtime grant. */
    public Set<String> devices(Directory directory, ControlUser user, String toolId) {
        var access=resolve(directory,user);
        if(!access.managed() && (user.access()==null || user.access().role()!=UserRole.BASIC)) return null;
        var devices=new TreeSet<String>();
        for(var grant:access.grants()) {
            if(!grant.templateIds().contains(UserPrivilegeTemplate.TOOL_USER) && !grant.templateIds().contains(UserPrivilegeTemplate.IT_CLOUD_ADMIN)) continue;
            if(!grant.toolIds().isEmpty() && !grant.toolIds().contains(toolId)) continue;
            if(grant.deviceScope()==RoleDeviceScope.ALL) return null;
            if(grant.deviceScope()!=RoleDeviceScope.GROUPS) continue;
            for(var groupId:grant.deviceGroupIds()) {
                var entry=directory.entries().get(Kind.TEAM.id(groupId));
                if(entry==null || !entry.enabled()) continue;
                var group=codec.model(entry.document(),ControlTeam.class);
                if(group.deviceIds()!=null) for(var id:group.deviceIds()) {
                    var device=directory.entries().get(Kind.DEVICE.id(id));
                    if(device!=null && device.enabled()) devices.add(id);
                }
            }
        }
        return devices;
    }
}
