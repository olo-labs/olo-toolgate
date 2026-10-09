// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
package io.ololabs.toolgate.control.application;
import io.ololabs.toolgate.contracts.*;
import com.fasterxml.jackson.databind.JsonNode;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Pattern;

/** Bounded data validation and scanning. No interpreter, image builder or author process exists here. */
public final class BuilderValidation {
    private BuilderValidation() {}
    private static final List<Pattern> SECRETS=List.of(
        Pattern.compile("-----BEGIN (?:RSA |EC |OPENSSH )?PRIVATE "+"KEY-----"),
        Pattern.compile("\\bAKIA[A-Z0-9]{16}\\b"),Pattern.compile("\\bgh[pousr]_[A-Za-z0-9]{36,}\\b"),
        Pattern.compile("\\b(?:sk|rk)-[A-Za-z0-9_-]{20,}\\b"),
        Pattern.compile("(?i)(?:password|passwd|api[_-]?key|access[_-]?token|client[_-]?secret)\\s*[=:]\\s*['\\\"][^'\\\"]{8,}['\\\"]"),
        Pattern.compile("eyJ[A-Za-z0-9_-]{10,}\\.[A-Za-z0-9_-]{10,}\\.[A-Za-z0-9_-]{10,}"));
    public static void scan(String text){if(text.length()>65536)throw Failure.validation();for(var pattern:SECRETS)if(pattern.matcher(text).find())throw new Failure(ErrorCode.VALIDATION,400,"Possible secret detected; use credential references");}
    private static void strings(JsonNode node){if(node.isTextual())scan(node.textValue());else if(node.isObject()){node.fieldNames().forEachRemaining(BuilderValidation::scan);node.elements().forEachRemaining(BuilderValidation::strings);}else if(node.isArray())node.elements().forEachRemaining(BuilderValidation::strings);}
    public static void source(ManagedRuntime runtime,LocalToolRegistration tool){
        if(tool.source()==null)return;
        if(!Set.of(LocalRuntimeKind.PYTHON,LocalRuntimeKind.NODE,LocalRuntimeKind.POWERSHELL,LocalRuntimeKind.SHELL).contains(runtime.kind()))throw new Failure(ErrorCode.UNSUPPORTED,400,"Inline source requires a supported script runtime");
        var code=tool.source().code();if(code.getBytes(StandardCharsets.UTF_8).length>8192||code.contains("\0")||!DirectoryService.digest(code).equals(tool.source().sha256())||tool.limits().maxInputBytes()>8192)throw Failure.validation();scan(code);
    }
    private static void schema(JsonNode node,int depth){
        if(depth>16||node.size()>64)throw Failure.validation();
        if(node.isObject()){
            for(var key:List.of("$ref","$dynamicRef","$id","$schema","pattern","patternProperties"))if(node.has(key))throw Failure.validation();
            node.elements().forEachRemaining(child->schema(child,depth+1));
        }else if(node.isArray())node.elements().forEachRemaining(child->schema(child,depth+1));
    }
    public static FleetPackageDocument validate(Codec codec,BuilderDefinition definition,boolean runnable){
        var tree=(JsonNode)codec.value(codec.json(definition));strings(tree);
        if(!definition.permissions().equals(List.of(BuilderPermission.COMPUTE))||definition.resource().kind()!=ResourceKind.CUSTOM||!definition.resource().locator().equals("runtime/"+definition.tool().toolId()))throw new Failure(ErrorCode.FORBIDDEN,403,"Unsupported permission or resource mapping");
        if(runnable&&!definition.credentialRequirements().isEmpty())throw new Failure(ErrorCode.UNSUPPORTED,400,"Credential binding is unavailable for isolated compute tools");
        if(definition.platforms().isEmpty()||definition.architectures().isEmpty()||definition.examples().isEmpty()||!definition.tool().runtimeId().equals(definition.runtime().id()))throw Failure.validation();
        source(definition.runtime(),definition.tool());
        InstalledProfiles.validate(definition.tool(),definition.runtime());
        var profile=definition.tool().authorizationProfile();
        if(!profile.tool().version().equals(definition.version())||profile.extractor().extractorKind()!=EnterpriseExtractorKind.FIELDS||!profile.extractor().fields().isEmpty()
            ||!profile.extractor().fixedResources().equals(List.of(definition.resource()))||profile.extractor().amountPointer()!=null||profile.extractor().operationPointer()!=null)throw Failure.validation();
        var factory=com.networknt.schema.JsonSchemaFactory.getInstance(com.networknt.schema.SpecVersion.VersionFlag.V202012);
        for(var field:List.of("inputSchema","outputSchema")){
            var value=tree.path("tool").path(field);if(!value.path("type").asText().equals("object")||!value.has("additionalProperties")||value.path("additionalProperties").asBoolean(true)||codec.json(value).length()>8192)throw Failure.validation();schema(value,0);
            try{var validator=factory.getSchema(value);for(var example:definition.examples()){
                if(!example.toolId().equals(definition.tool().toolId()))throw Failure.validation();
                var sample=field.equals("inputSchema")?example.arguments():example.expectedOutput();
                if(!validator.validate((JsonNode)codec.value(codec.json(sample))).isEmpty())throw Failure.validation();
            }}catch(Failure failure){throw failure;}catch(RuntimeException failure){throw Failure.validation();}
        }
        var document=new FleetPackageDocument(1L,definition.packageId(),definition.version(),definition.platforms(),definition.architectures(),"0.10.0-dev",List.of(definition.runtime()),List.of(definition.tool()),List.of(definition.examples().getFirst()));
        FleetService.validatePackage(document);if(codec.json(document).getBytes(StandardCharsets.UTF_8).length>32768)throw Failure.validation();return document;
    }
}
