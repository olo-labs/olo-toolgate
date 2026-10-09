// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
package io.ololabs.toolgate.control.application;

import io.ololabs.toolgate.control.domain.*;
import java.util.List;

/** Canonical group graph publication. A graph snapshot never grants execution from cached scopes. */
public final class PolicyCompiler {
    private final Codec codec;
    public PolicyCompiler(Codec codec) {this.codec=codec;}
    public String compile(Ids.TenantId tenant,Directory directory) {
        directory.validate(512,1048576);codec.validatePolicies(directory);
        var document=codec.snapshot(tenant,directory,false);
        if(document.getBytes(java.nio.charset.StandardCharsets.UTF_8).length>1048576)throw Failure.validation();
        return document;
    }
}
