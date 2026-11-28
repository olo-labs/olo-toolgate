// Copyright 2026 OLO Labs; SPDX-License-Identifier: Apache-2.0
package io.ololabs.bots.business;

import io.ololabs.toolgate.sdk.annotations.*;
import io.ololabs.toolgate.sdk.context.ToolContext;
import io.ololabs.toolgate.sdk.errors.ToolBusinessError;
import java.util.List;
import java.util.Map;
import java.util.Set;

@ToolPackage(id="olo.business", name="Business document processing", publisher="OLO Labs",
        description="Bounded document extraction and deterministic business checks through a reviewed processor route.")
@Destination(id="processor", hostSetting="ProcessorHost", port=8443)
@Global(name="ProcessorHost", type=ValueType.STRING, defaultValue="business-processor",
        pattern="^[A-Za-z0-9.-]{1,253}$", description="Operator-owned document processor DNS name")
@HttpAuth(id="processor-key",type=AuthType.API_KEY,header="X-Api-Key",secret="businessprocessorapikey")
@HttpRoute(id="submit",destination="processor",method=HttpMethod.POST,path="/v1/submit",
        auth="processor-key",maxRequestBytes=4096,maxResponseBytes=1_048_576,timeoutMs=5000)
@HttpRoute(id="result",destination="processor",method=HttpMethod.POST,path="/v1/result",
        auth="processor-key",maxRequestBytes=4096,maxResponseBytes=1_048_576,timeoutMs=5000)
public final class BusinessTools {
    public record Evidence(
        @Param(description="Source identifier") String sourceId,
        @Param(description="Document identifier; empty for a structured pointer") String documentId,
        @Param(description="One-based page; zero for a structured pointer") int page,
        @Param(description="One-based line; zero for a structured pointer") int line,
        @Param(description="Exact source quote; empty for a structured pointer") String quote,
        @Param(description="Original document SHA-256; empty for a structured pointer") String sha256,
        @Param(description="Extraction or deterministic-rule engine") String engine,
        @Param(description="Applied right-angle orientation correction") int rotation,
        @Param(description="JSON pointer for structured evidence; empty for source text") String pointer) {}
    public record Finding(
        @Param(description="Stable rule/check identifier") String checkId,
        @Param(description="pass, fail, needs review or unsupported") String status,
        @Param(description="Deterministic reason") String detail,
        @Param(description="Observed source values, preserving conflicts") List<String> values,
        @Param(description="Supporting source locations") List<Evidence> evidence) {}
    public record Report(
        @Param(description="Deterministic report description") String summary,
        @Param(description="Findings; independent from execution state") List<Finding> findings,
        @Param(description="Reviewable draft; never sent by processing") String draft,
        @Param(description="Outstanding reviewer work") List<String> nextActions) {}
    public record Source(
        @Param(description="Source page identifier") String sourceId,
        @Param(description="Document identifier") String documentId,
        @Param(description="One-based page") int page,
        @Param(description="Extracted source text") String text,
        @Param(description="Original document SHA-256") String sha256,
        @Param(description="Extraction engine") String engine,
        @Param(description="Applied orientation correction") int rotation) {}
    public record Result(
        @Param(description="Deterministic business report") Report report,
        @Param(description="Bounded extracted source pages") List<Source> sources) {}
    public record IntakeResult(
        @Param(description="Processed document evidence and deterministic report") Result processing,
        @Param(description="Authorized intake identifier") String intakeId,
        @Param(description="Exact observed intake manifest SHA-256") String manifestSha256) {}

    public record JobStatus(String state,String intakeId,String manifestSha256,Result processing) {}

    @Authorization(action="document.process",resources="documents://{tenant}/intake/{intakeId}")
    @ServerTool(name="process-intake",title="Process an authorized document intake",
        description="Submit a bounded source-pinned document job and collect its confirmed result. Submission and each result access record a fenced target receipt. No payment, account, model or approval action occurs.",
        effect=EffectSafety.NON_IDEMPOTENT,stateless=false,reconciliation=Reconciliation.MANUAL,
        routes={"submit","result"},maxDurationMs=60_000,
        errors={@ErrorDecl(code="INVALID_REQUEST",description="Invalid intake or use case"),
                @ErrorDecl(code="SOURCE_UNAVAILABLE",description="An authorized source is unavailable"),
                @ErrorDecl(code="SOURCE_CHANGED",description="Source bytes differ from the observed manifest")})
    @Example(arguments="{\"useCase\":\"unknown\",\"intakeId\":\"sample\"}",expectedError="INVALID_REQUEST")
    public IntakeResult processIntake(
        @Param(description="ocr, medical, vlsic, loan, invoice, support, itsm or onboarding",maxLength=32) String useCase,
        @Param(description="Opaque intake identifier from the approved repository",minLength=1,maxLength=96,pattern="^[A-Za-z0-9_-]+$") String intakeId,
        ToolContext context) {
        if(useCase==null||!Set.of("ocr","medical","vlsic","loan","invoice","support","itsm","onboarding").contains(useCase)
                ||intakeId==null||!intakeId.matches("[A-Za-z0-9_-]{1,96}"))
            throw new ToolBusinessError("INVALID_REQUEST","Unsupported business use case");
        String tenant=context.invocation().tenantId().orElseThrow(()->new SecurityException("Verified tenant required"));
        var body=Map.of("tenantId",tenant,"useCase",useCase,"intakeId",intakeId);
        // Never retry submission or an uncertain result-access receipt.
        var status=context.http().route("submit").effectKey("document.submit").body(body).send(JobStatus.class);
        String manifest=status.manifestSha256();
        for(int poll=0;poll<=90;poll++) {
            if(!intakeId.equals(status.intakeId())||manifest==null||!manifest.matches("sha256:[a-f0-9]{64}")
                    ||!manifest.equals(status.manifestSha256()))
                throw new ToolBusinessError("SOURCE_CHANGED","Document job returned another intake or source version");
            if("READY".equals(status.state())&&status.processing()!=null)
                return new IntakeResult(status.processing(),status.intakeId(),manifest);
            if(!Set.of("QUEUED","RUNNING").contains(status.state())||status.processing()!=null)
                throw new ToolBusinessError("INVALID_REQUEST","Document job requires operator review");
            if(poll==90)break;
            try {Thread.sleep(500);}catch(InterruptedException cancelled){Thread.currentThread().interrupt();throw new IllegalStateException("Document wait cancelled",cancelled);}
            status=context.http().route("result").effectKey("document.result."+poll).body(body).send(JobStatus.class);
        }
        throw new ToolBusinessError("INVALID_REQUEST","Document job remains pending; inspect its existing identity before another operation");
    }
}
