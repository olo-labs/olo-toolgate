// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0

namespace OloLabs.ToolGate.Sdk;

// Enum members are PascalCase; the descriptor always uses the neutral upper-snake spelling
// (EffectSafety.NonIdempotent is NON_IDEMPOTENT). The source generator does the mapping.

/// <summary>How a route authenticates (http.md §5).</summary>
public enum AuthType { ApiKey, Basic, OAuth2ClientCredentials, TokenExchange, Connection, Custom }

/// <summary>Whether a business key is required; <see cref="Default"/> is true for a NonIdempotent destructive tool.</summary>
public enum BusinessKey { Default, Required, NotRequired }

/// <summary>Effect safety of a tool (execution.md §4).</summary>
public enum EffectSafety { ReadOnly, Idempotent, NonIdempotent, Unknown }

/// <summary>HTTP method of a route.</summary>
public enum HttpMethod { Get, Head, Post, Put, Patch, Delete }

/// <summary>How the business key is forwarded upstream.</summary>
public enum IdempotencyScope { BusinessKey, PerInvocation }

/// <summary>Business key namespace (execution.md §4).</summary>
public enum KeyNamespace { Agent, DelegatedUser, Tenant }

/// <summary>MCP openWorldHint; <see cref="Default"/> is true for a server tool with routes and false otherwise.</summary>
public enum OpenWorld { Default, True, False }

/// <summary>Endpoint runtime tier of a client tool.</summary>
public enum RuntimeTier { OciSandbox, Wasi, HostNative }

/// <summary>Who a secret belongs to.</summary>
public enum SecretScope { Group, User }

/// <summary>Streaming mode of a route.</summary>
public enum Streaming { None, Response, Bidirectional }

/// <summary>Value type (authoring README §7); <see cref="Infer"/> takes the type from the .NET type.</summary>
public enum ValueType { Infer, String, Int, Long, Number, Boolean, Date, DateTime, Duration, Enum, Uri, Object, Array, Binary }
