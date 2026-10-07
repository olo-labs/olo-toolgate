// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
//! Stateless MCP ingress with an optional device-scoped client relay.
use crate::http::{error, parse, single_header, status, AppState};
use crate::unix_ms;
use axum::{
    extract::{Request, State},
    http::{HeaderMap, StatusCode},
    response::{IntoResponse, Response},
    Json,
};
use base64::Engine;
use olo_toolgate_contracts::{
    AuthorizationRequest, Decision, ErrorCode, PolicyInput, RemoteToolState, RemoteToolSubmission,
    ResourceKind,
};
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
    let credential_deadline = request
        .extensions()
        .get::<crate::http::CredentialDeadline>()
        .map(|d| d.0)
        .unwrap_or(0);
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
            json!({"supportedVersions":["2026-07-28"],"capabilities":if s.relay.is_some(){json!({"tools":{}})}else{json!({})},"ttlMs":0,"cacheScope":"private"}),
        ),
        "tools/list" => {
            let Some(relay) = &s.relay else {
                return success(id, json!({"tools":[],"ttlMs":0,"cacheScope":"private"}));
            };
            match relay.catalog(&context).await {
                Ok(catalog) => success(
                    id,
                    json!({"tools":catalog.tools.iter().filter(|tool|tool.enabled).map(|tool|json!({"name":tool.tool_id,"description":tool.description,"inputSchema":tool.input_schema})).collect::<Vec<_>>(),"ttlMs":0,"cacheScope":"private"}),
                ),
                Err(code) => rpc_error(
                    status(&code),
                    id,
                    -32000,
                    "Local tool discovery failed",
                    json!({"code":code}),
                ),
            }
        }
        "tools/call" => {
            let name = params["name"].as_str().unwrap_or("");
            let action = if let Some(relay) = &s.relay {
                let catalog = match relay.catalog(&context).await {
                    Ok(catalog) => catalog,
                    Err(code) => return error(code, &correlation.request_id),
                };
                let matches: Vec<_> = catalog
                    .tools
                    .into_iter()
                    .filter(|tool| tool.enabled && tool.tool_id == name)
                    .collect();
                if matches.len() != 1 {
                    return rpc_error(
                        StatusCode::FORBIDDEN,
                        id,
                        -32602,
                        "Tool unavailable for this agent and client",
                        Value::Null,
                    );
                }
                matches[0].action.clone()
            } else {
                let Some(action) = s.gateway.extractors.mcp_action(name) else {
                    return rpc_error(
                        StatusCode::BAD_REQUEST,
                        id,
                        -32602,
                        "Unknown or ambiguous tool",
                        Value::Null,
                    );
                };
                action.to_owned()
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
            if !s.gateway.contracts.valid(
                "AuthorizationRequest",
                &serde_json::to_value(&request).unwrap_or_default(),
            ) {
                return error(ErrorCode::Validation, &correlation.request_id);
            }
            let Some(now) = unix_ms() else {
                return error(ErrorCode::DependencyUnavailable, &correlation.request_id);
            };
            let mut context = context;
            let mut authorized = request.clone();
            let mut managed_runtime = false;
            if s.relay.is_some() {
                let builtins: Value = serde_json::from_str(include_str!(
                    "../../../packages/contracts/tools/builtins.json"
                ))
                .unwrap_or(Value::Null);
                let builtin = builtins["tools"].as_array().is_some_and(|tools| {
                    tools.iter().any(|tool| tool["toolId"] == request.tool_id)
                });
                if builtin {
                    authorized
                        .arguments
                        .entry("path".into())
                        .or_insert(json!("hotfolder"));
                } else {
                    managed_runtime = true;
                    authorized.arguments=serde_json::from_value(json!({"path":format!("runtime/{}",request.tool_id),"input":request.arguments})).unwrap_or_default();
                }
                context.request_id = format!(
                    "mcp-{}",
                    &crate::digest(
                        &serde_json::to_vec(&(
                            &context.tenant_id,
                            &context.user_id,
                            &context.agent_id,
                            &context.device_id,
                            &id,
                            &request
                        ))
                        .unwrap_or_default()
                    )[..48]
                );
            }
            let input = if managed_runtime {
                let binding = crate::extraction::ExtractorBinding {
                    tool_id: authorized.tool_id.clone(),
                    action: authorized.action.clone(),
                    pointer: "/path".into(),
                    kind: ResourceKind::Custom,
                };
                let resource = match crate::extraction::ResourceExtractor::extract(
                    &binding,
                    &serde_json::to_value(&authorized.arguments).unwrap_or_default(),
                ) {
                    Ok(resource) => resource,
                    Err(_) => return error(ErrorCode::Validation, &correlation.request_id),
                };
                Ok(PolicyInput {
                    context: context.clone(),
                    tool_id: authorized.tool_id.clone(),
                    action: authorized.action.clone(),
                    resource,
                    arguments_digest: crate::digest(
                        &serde_json::to_vec(&authorized.arguments).unwrap_or_default(),
                    ),
                })
            } else {
                s.gateway.normalize(authorized, context.clone())
            };
            let input = match input {
                Ok(input) => input,
                Err(code) => return error(code, &correlation.request_id),
            };
            match s
                .gateway
                .authorize_input(input.clone(), correlation.trace_id, now)
                .await
            {
                Ok(decision) if decision.decision == Decision::Allow => {
                    s.metrics.allow.fetch_add(1, Ordering::Relaxed);
                    if let Some(relay) = &s.relay {
                        let expires = credential_deadline.min(now.saturating_add(
                            s.config.limits.request_timeout_ms.saturating_sub(1000),
                        ));
                        if expires <= now.saturating_add(2000) {
                            return error(ErrorCode::Timeout, &correlation.request_id);
                        }
                        let submission = RemoteToolSubmission {
                            input,
                            request,
                            expires_at_unix_ms: expires,
                        };
                        let mut response = match relay.submit(&submission).await {
                            Ok(response) => response,
                            Err(code) => {
                                return rpc_error(
                                    status(&code),
                                    id,
                                    -32000,
                                    "Local tool request rejected",
                                    json!({"code":code}),
                                )
                            }
                        };
                        loop {
                            if response.record.request_id != context.request_id
                                || response.record.device_id
                                    != context.device_id.clone().unwrap_or_default()
                                || response.record.agent_id != context.agent_id
                                || response.record.tool_id != submission.request.tool_id
                            {
                                return error(ErrorCode::Validation, &correlation.request_id);
                            }
                            if unix_ms().is_none_or(|time| time >= expires) {
                                return error(ErrorCode::Timeout, &correlation.request_id);
                            }
                            match response.record.state {
                                RemoteToolState::Done => {
                                    let Some(result) = response.result else {
                                        return error(
                                            ErrorCode::Validation,
                                            &correlation.request_id,
                                        );
                                    };
                                    if result.request_id != context.request_id
                                        || result.error.is_some()
                                    {
                                        return error(
                                            ErrorCode::Validation,
                                            &correlation.request_id,
                                        );
                                    }
                                    let Some(output) = result.output else {
                                        return error(
                                            ErrorCode::Validation,
                                            &correlation.request_id,
                                        );
                                    };
                                    return success(
                                        id,
                                        json!({"content":[{"type":"text","text":serde_json::to_string(&output).unwrap_or_default()}],"structuredContent":output,"isError":false}),
                                    );
                                }
                                RemoteToolState::Failed | RemoteToolState::Expired => {
                                    return rpc_error(
                                        StatusCode::OK,
                                        id,
                                        -32000,
                                        "Local tool execution failed",
                                        json!({"code":response.record.error.unwrap_or(ErrorCode::Timeout),"requestId":context.request_id}),
                                    )
                                }
                                _ => {}
                            }
                            tokio::time::sleep(std::time::Duration::from_millis(250)).await;
                            response = match relay.response(&context).await {
                                Ok(response) => response,
                                Err(code) => {
                                    return rpc_error(
                                        status(&code),
                                        id,
                                        -32000,
                                        "Local tool response unavailable",
                                        json!({"code":code}),
                                    )
                                }
                            };
                        }
                    }
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
