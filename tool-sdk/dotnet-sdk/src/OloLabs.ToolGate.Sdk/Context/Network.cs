// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0

using System;
using System.Collections.Generic;
using System.IO;
using System.Threading.Tasks;

namespace OloLabs.ToolGate.Sdk;

/// <summary>Route client. There is no free-form URL, base URL or host API.</summary>
public interface IHttpClient
{
    IRouteCall Route(string routeId);
    /// <summary>Convenience form; the destination and template must be literals that match exactly one of the tool's routes.</summary>
    IRouteCall Get(string destinationId, string pathTemplate);
    IRouteCall Post(string destinationId, string pathTemplate);
    IRouteCall Put(string destinationId, string pathTemplate);
    IRouteCall Patch(string destinationId, string pathTemplate);
    IRouteCall Delete(string destinationId, string pathTemplate);
    IRouteCall Head(string destinationId, string pathTemplate);
}

/// <summary>One call on a declared route.</summary>
public interface IRouteCall
{
    IRouteCall PathVar(string name, object? value);
    /// <summary>A null value is omitted.</summary>
    IRouteCall Query(string name, object? value);
    /// <summary>The header must be in the route's allowlist.</summary>
    IRouteCall Header(string name, string value);
    IRouteCall Body(object? value);
    /// <summary>May only shorten the route timeout.</summary>
    IRouteCall Timeout(TimeSpan timeout);
    /// <summary>Sends the call and reads the JSON response body as <typeparamref name="T"/>.</summary>
    Task<T> SendAsync<T>();
    Task<RawResponse> SendRawAsync();
}

/// <summary>A response with its bytes and headers.</summary>
public sealed class RawResponse
{
    public RawResponse(int status, IReadOnlyDictionary<string, IReadOnlyList<string>> headers, byte[] body)
    {
        Status = status;
        Headers = headers;
        Body = body;
    }

    public int Status { get; }
    /// <summary>Header names are lower case.</summary>
    public IReadOnlyDictionary<string, IReadOnlyList<string>> Headers { get; }
    public byte[] Body { get; }
}

/// <summary>Connections to declared TCP destinations.</summary>
public interface ITcpConnector
{
    Task<Stream> ConnectAsync(string destinationId);
}
