// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
package io.ololabs.toolgate.control.application;

import com.fasterxml.jackson.databind.JsonNode;
import io.ololabs.toolgate.contracts.*;
import java.net.URI;
import java.text.Normalizer;
import java.util.*;

/** Bounded, reviewed extraction. All argument-derived resources are normalized before authorization. */
public final class ResourceExtraction {
    private final Codec codec;
    public ResourceExtraction(Codec codec) {this.codec=codec;}
    public record Extracted(List<ResourceDescriptor> resources,Long amount,String operation,String argumentsDigest) {}
    public Extracted extract(ControlResourceExtractor extractor,AuthorizationRequest request) {
        var arguments=(JsonNode)codec.value(codec.json(request.arguments()));
        var resources=new TreeMap<String,ResourceDescriptor>();
        for(var resource:extractor.fixedResources())add(resources,resource.kind(),resource.locator());
        for(var field:extractor.fields()) {
            var value=pointer(arguments,field.pointer());
            if(field.multiple()) {
                if(!value.isArray()||value.isEmpty()||value.size()>extractor.maxResources())throw Failure.validation();
                for(var item:value) {if(!item.isTextual())throw Failure.validation();add(resources,field.kind(),item.asText());}
            }else {if(!value.isTextual())throw Failure.validation();add(resources,field.kind(),value.asText());}
        }
        if(resources.isEmpty()||resources.size()>extractor.maxResources())throw Failure.validation();
        Long amount=null;String operation=null;
        if(extractor.amountPointer()!=null) {var value=pointer(arguments,extractor.amountPointer());if(!value.isIntegralNumber()||!value.canConvertToLong()||value.asLong()<0)throw Failure.validation();amount=value.asLong();}
        if(extractor.operationPointer()!=null) {var value=pointer(arguments,extractor.operationPointer());if(!value.isTextual()||!value.asText().matches("[a-zA-Z][a-zA-Z0-9._-]{0,127}"))throw Failure.validation();operation=value.asText();}
        if(extractor.extractorKind()==EnterpriseExtractorKind.SQL) {
            // SQL authorizes a reviewed structured operation; arbitrary SQL text cannot masquerade as that operation.
            if(operation==null||arguments.has("sql")||arguments.has("query")||arguments.has("statement"))throw Failure.validation();
        }
        if(extractor.extractorKind()==EnterpriseExtractorKind.SHELL && (!resources.values().stream().allMatch(r->r.kind()==ResourceKind.CUSTOM)||operation==null))throw Failure.validation();
        return new Extracted(List.copyOf(resources.values()),amount,operation,DirectoryService.digest(codec.json(request.arguments())));
    }
    private static JsonNode pointer(JsonNode arguments,String pointer) {
        if(!pointer.startsWith("/")||pointer.contains("//")||pointer.matches(".*~(?![01]).*"))throw Failure.validation();
        var value=arguments.at(pointer);if(value.isMissingNode()||value.isNull())throw Failure.validation();return value;
    }
    private void add(Map<String,ResourceDescriptor> resources,ResourceKind kind,String locator) {
        var resource=new ResourceDescriptor(kind,normalize(kind,locator));resources.put(codec.json(resource),resource);
    }
    public static String normalize(ResourceKind kind,String value) {
        if(value==null||value.isEmpty()||value.length()>4096||value.codePoints().anyMatch(c->Character.isISOControl(c)||Character.getType(c)==Character.FORMAT)||!Normalizer.isNormalized(value,Normalizer.Form.NFC))throw Failure.validation();
        if(kind==ResourceKind.FILE) {
            if(value.startsWith("/")||value.contains("\\")||value.contains(":")||value.matches("(?i).*%(2e|2f|5c|00).*"))throw Failure.validation();
            var segments=value.split("/",-1);if(Arrays.stream(segments).anyMatch(s->s.isEmpty()||s.equals(".")||s.equals("..")||s.endsWith(".")||s.endsWith(" ")||s.matches("(?i)(con|prn|aux|nul|com[1-9]|lpt[1-9])(\\..*)?")))throw Failure.validation();return value;
        }
        if(kind==ResourceKind.URL) {
            try {
                var uri=URI.create(value);if(!List.of("https","http").contains(uri.getScheme())||uri.getHost()==null||uri.getUserInfo()!=null||uri.getFragment()!=null||uri.getPort()>65535||!uri.normalize().equals(uri)||value.matches("(?i).*%(2e|2f|5c|00).*"))throw Failure.validation();
                var scheme=uri.getScheme().toLowerCase(Locale.ROOT);var host=uri.getHost().toLowerCase(Locale.ROOT);var port=uri.getPort();if(port==(scheme.equals("https")?443:80))port=-1;
                return new URI(scheme,null,host,port,uri.getPath().isEmpty()?"/":uri.getPath(),uri.getQuery(),null).toASCIIString();
            }catch(java.net.URISyntaxException|IllegalArgumentException failure){throw Failure.validation();}
        }
        if(kind==ResourceKind.DATABASE && !value.matches("[a-zA-Z0-9][a-zA-Z0-9._:/-]{0,255}"))throw Failure.validation();
        return value;
    }
}
