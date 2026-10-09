// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
package io.ololabs.toolgate.control.application;

import io.ololabs.toolgate.control.domain.*;
import java.util.*;

/** Device membership helpers; ownership and default membership confer no permission. */
public final class DeviceGroups {
    public static final String DEFAULT="default-devices";
    private DeviceGroups() {}
    public static void addDefault(Map<Ids.RecordId,Directory.Entry> entries,Codec codec,String device) { GroupGraph.addDefault(entries,codec,Ids.Kind.DEVICE,device); }
    public static Set<String> members(Directory directory,Codec codec,String id) {
        var group=directory.entries().get(Ids.Kind.DEVICE_GROUP.id(id));if(group==null||!group.enabled())return Set.of();var result=new TreeSet<String>();
        for(var member:GroupGraph.members(group,codec)){var e=directory.entries().get(Ids.Kind.DEVICE.id(member));if(e!=null&&e.enabled())result.add(member);}
        return Set.copyOf(result);
    }
}
