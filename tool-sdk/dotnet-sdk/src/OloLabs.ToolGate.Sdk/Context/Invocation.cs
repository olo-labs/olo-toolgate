// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0

using System;
using System.Collections.Generic;
using System.Linq;
using System.Text.Json;
using System.Text.Json.Nodes;

namespace OloLabs.ToolGate.Sdk;

/// <summary>Diagnostics only; must not change behaviour.</summary>
public enum InvocationMode { Standalone, Governed }

/// <summary>The current invocation.</summary>
public sealed class Invocation
{
    public Invocation(string invocationId, string toolId, string? agentId, string? userId, long? catalogRevision,
        DateTimeOffset deadline, string? businessKey, int attempt, InvocationMode mode)
    {
        InvocationId = invocationId;
        ToolId = toolId;
        AgentId = agentId;
        UserId = userId;
        CatalogRevision = catalogRevision;
        Deadline = deadline;
        BusinessKey = businessKey;
        Attempt = attempt;
        Mode = mode;
    }

    public string InvocationId { get; }
    public string ToolId { get; }
    public string? AgentId { get; }
    public string? UserId { get; }
    public long? CatalogRevision { get; }
    public DateTimeOffset Deadline { get; }
    public string? BusinessKey { get; }
    public int Attempt { get; }
    public InvocationMode Mode { get; }
}

/// <summary>A question for the caller: an MRTR round in sync calls, <c>input_required</c> in tasks.</summary>
/// <typeparam name="T">The answer type, read from the answer's JSON.</typeparam>
public sealed class InputRequest<T>
{
    private InputRequest(string message, JsonObject requestedSchema)
    {
        Message = message;
        RequestedSchema = requestedSchema;
    }

    public static InputRequest<T> Of(string message, JsonObject requestedSchema) =>
        new(message, (JsonObject)requestedSchema.DeepClone());

    /// <summary>The requested schema as a JSON object text.</summary>
    public static InputRequest<T> Of(string message, string requestedSchemaJson) =>
        new(message, JsonNode.Parse(requestedSchemaJson) as JsonObject
            ?? throw new ArgumentException("The requested schema must be a JSON object.", nameof(requestedSchemaJson)));

    public string Message { get; }
    public JsonObject RequestedSchema { get; }
    public Type AnswerType => typeof(T);
}

/// <summary>An explicit result: MCP content plus optional structured content.</summary>
public sealed class ToolResult
{
    private ToolResult(IReadOnlyList<JsonObject> content, JsonObject? structuredContent)
    {
        Content = content;
        StructuredContent = structuredContent;
    }

    public static ToolResult Of(IEnumerable<JsonObject> content, JsonObject? structuredContent = null) =>
        new(content.Select(c => (JsonObject)c.DeepClone()).ToList(), (JsonObject?)structuredContent?.DeepClone());

    public static ToolResult Text(string text) =>
        new(new List<JsonObject> { new() { ["type"] = "text", ["text"] = text } }, null);

    public IReadOnlyList<JsonObject> Content { get; }
    public JsonObject? StructuredContent { get; }

    /// <summary>The content as JSON, for diagnostics.</summary>
    public override string ToString() => new JsonArray(Content.Select(c => (JsonNode)c.DeepClone()).ToArray())
        .ToJsonString(new JsonSerializerOptions { WriteIndented = false });
}
