// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
//! Stateless MCP ingress skeleton; no executable capabilities are advertised.
use crate::http::{error, parse, single_header, status, AppState};
use crate::unix_ms;
use axum::{
    extract::{Request, State},
    http::{HeaderMap, StatusCode},
    response::{IntoResponse, Response},
    Json,
};
use base64::Engine;
use olo_toolgate_contracts::{AuthorizationRequest, Decision, ErrorCode};
use serde_json::{json, Value};
use std::sync::{atomic::Ordering, Arc};
pub(crate) fn rpc_error(
    http_status: StatusCode,
    id: Value,
    code: i64,
    message: &'static str,
    data: Value,
) -> Response {
    let body =
        json!({"jsonrpc":"2.0", "id":id, "error":{"code":code,"message":message,"data":data}});
    (http_status, Json(body)).into_response()
}

fn decoded_name(headers: &HeaderMap) -> Option<String> {
    let value = single_header(headers, "mcp-name")?;
    if let Some(encoded) = value
        .strip_prefix("=?base64?")
        .and_then(|v| v.strip_suffix("?="))
    {
        String::from_utf8(
            base64::engine::general_purpose::STANDARD
                .decode(encoded)
                .ok()?,
        )
        .ok()
    } else {
        Some(value.into())
    }
}

pub(crate) async fn ingress(State(s): State<Arc<AppState>>, request: Request) -> Response {
    let (value, context, correlation, headers) = match parse(&s, request).await {
        Ok(v) => v,
        Err(r) => return r,
    };
    let id = value.get("id").cloned().unwrap_or(Value::Null);
    let Some(object) = value.as_object() else {
        return rpc_error(
            StatusCode::BAD_REQUEST,
            Value::Null,
            -32600,
            "Invalid request",
            Value::Null,
        );
    };
    if value["jsonrpc"] == "2.0" && value["method"].is_string() && !object.contains_key("id") {
        // Notifications have no JSON-RPC reply. This skeleton rejects the
        // unsupported transport operation with an ordinary HTTP error instead.
        return error(ErrorCode::Unsupported, &correlation.request_id);
    }
    if value["jsonrpc"] != "2.0"
        || !(id.is_string() || id.is_i64() || id.is_u64())
        || id.as_str().is_some_and(|v| v.len() > 128)
        || object
            .keys()
            .any(|k| !["jsonrpc", "id", "method", "params"].contains(&k.as_str()))
        || !value["params"].is_object()
    {
        return rpc_error(
            StatusCode::BAD_REQUEST,
            Value::Null,
            -32600,
            "Invalid request",
            Value::Null,
        );
    }
    let version = single_header(&headers, "mcp-protocol-version");
    let method = value["method"].as_str().unwrap_or("");
    let params = &value["params"];
    if version.is_none()
        || version != params["_meta"]["io.modelcontextprotocol/protocolVersion"].as_str()
        || single_header(&headers, "mcp-method") != Some(method)
        || (method == "tools/call" && decoded_name(&headers).as_deref() != params["name"].as_str())
    {
        return rpc_error(
            StatusCode::BAD_REQUEST,
            id,
            -32020,
            "Header mismatch",
            Value::Null,
        );
    }
    if version != Some("2026-07-28") {
        return rpc_error(
            StatusCode::BAD_REQUEST,
            id,
            -32022,
            "Unsupported protocol version",
            json!({"supported":["2026-07-28"],"requested":version}),
        );
    }
    let meta = &params["_meta"];
    if !meta["io.modelcontextprotocol/clientCapabilities"].is_object() {
        return rpc_error(
            StatusCode::BAD_REQUEST,
            id,
            -32602,
            "Client capabilities required",
            Value::Null,
        );
    }
    let accept = single_header(&headers, "accept").unwrap_or("");
    if !accept.split(',').any(|v| v.trim() == "application/json")
        || !accept.split(',').any(|v| v.trim() == "text/event-stream")
    {
        return rpc_error(
            StatusCode::NOT_ACCEPTABLE,
            id,
            -32600,
            "JSON and SSE acceptance required",
            Value::Null,
        );
    }
    match method {
        "ping" => success(id, json!({})),
        "server/discover" => success(
            id,
            json!({"supportedVersions":["2026-07-28"],"capabilities":{},"ttlMs":0,"cacheScope":"private"}),
        ),
        "tools/list" => success(id, json!({"tools":[],"ttlMs":0,"cacheScope":"private"})),
        "tools/call" => {
            let name = params["name"].as_str().unwrap_or("");
            let Some(action) = s.gateway.extractors.mcp_action(name) else {
                return rpc_error(
                    StatusCode::BAD_REQUEST,
                    id,
                    -32602,
                    "Unknown or ambiguous tool",
                    Value::Null,
                );
            };
            if params.as_object().is_none_or(|p| {
                p.keys()
                    .any(|k| !["name", "arguments", "_meta"].contains(&k.as_str()))
            }) {
                return rpc_error(
                    StatusCode::BAD_REQUEST,
                    id,
                    -32602,
                    "Invalid parameters",
                    Value::Null,
                );
            }
            let request = match serde_json::from_value::<AuthorizationRequest>(
                json!({"toolId":name,"action":action,"arguments":params["arguments"]}),
            ) {
                Ok(v) => v,
                Err(_) => {
                    return rpc_error(
                        StatusCode::BAD_REQUEST,
                        id,
                        -32602,
                        "Invalid parameters",
                        Value::Null,
                    )
                }
            };
            let Some(now) = unix_ms() else {
                return error(ErrorCode::DependencyUnavailable, &correlation.request_id);
            };
            match s
                .gateway
                .authorize(request, context, correlation.trace_id, now)
                .await
            {
                Ok(decision) if decision.decision == Decision::Allow => {
                    s.metrics.allow.fetch_add(1, Ordering::Relaxed);
                    rpc_error(
                        StatusCode::OK,
                        id,
                        -32601,
                        "Execution unsupported",
                        json!({"code":"UNSUPPORTED"}),
                    )
                }
                Ok(_) => {
                    s.metrics.block.fetch_add(1, Ordering::Relaxed);
                    rpc_error(
                        StatusCode::OK,
                        id,
                        -32000,
                        "Authorization blocked",
                        json!({"code":"FORBIDDEN"}),
                    )
                }
                Err(code) => rpc_error(
                    status(&code),
                    id,
                    -32000,
                    "Authorization failed",
                    json!({"code":code}),
                ),
            }
        }
        _ => rpc_error(
            StatusCode::NOT_FOUND,
            id,
            -32601,
            "Method not found",
            Value::Null,
        ),
    }
}

fn success(id: Value, mut result: Value) -> Response {
    result["resultType"] = json!("complete");
    result["_meta"] = json!({"io.modelcontextprotocol/serverInfo":{"name":"olo-toolgate-gateway","version":env!("CARGO_PKG_VERSION")}});
    Json(json!({"jsonrpc":"2.0","id":id,"result":result})).into_response()
}
