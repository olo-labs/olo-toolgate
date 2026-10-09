// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
//! Deployment definitions pin actual code and extraction versions without granting access.
use crate::{contracts::Contracts, Failure, Result};
use olo_toolgate_contracts::*;
use serde_json::{json, Value};

pub fn tool_digest(profile: &InstalledAuthorizationProfile) -> Result<String> {
    let canonical = |v: &Value| serde_json::to_string(v).map_err(|_| Failure::Validation);
    Ok(crate::digest(
        format!(
            "{}\n{}\n{}\n{}",
            canonical(
                &serde_json::to_value(&profile.tool.definition).map_err(|_| Failure::Validation)?
            )?,
            profile.tool.version,
            profile.tool.package_digest,
            canonical(&serde_json::to_value(&profile.extractor).map_err(|_| Failure::Validation)?)?
        )
        .as_bytes(),
    ))
}
pub fn validate(profile: &InstalledAuthorizationProfile) -> Result<()> {
    Contracts::shared()?.encode("InstalledAuthorizationProfile", profile)?;
    if !profile.tool.enabled
        || !profile.extractor.enabled
        || profile.tool.id != profile.tool.definition.id
        || profile.tool.extractor_id != profile.extractor.id
    {
        return Err(Failure::Validation);
    }
    Ok(())
}
pub fn builtin_package_digest() -> Result<String> {
    // Read the installed executable, including bytes beyond misleading metadata lengths.
    use std::io::Read;
    let path = std::env::current_exe().map_err(|_| Failure::Unavailable)?;
    let file = std::fs::File::open(path).map_err(|_| Failure::Unavailable)?;
    let mut bytes = Vec::new();
    file.take(268435457)
        .read_to_end(&mut bytes)
        .map_err(|_| Failure::Unavailable)?;
    if bytes.is_empty() || bytes.len() > 268435456 {
        return Err(Failure::Validation);
    }
    Ok(crate::digest(&bytes))
}
/// Export installation metadata for an operator to review and register. These profiles contain no grants.
pub fn builtin_profiles() -> Result<Vec<InstalledAuthorizationProfile>> {
    let registry: Value = serde_json::from_str(include_str!(
        "../../../packages/contracts/tools/builtins.json"
    ))
    .map_err(|_| Failure::Validation)?;
    let package = builtin_package_digest()?;
    registry["tools"].as_array().ok_or(Failure::Validation)?.iter().map(|item|{
        let id=item["toolId"].as_str().ok_or(Failure::Validation)?;
        let extractor_id=format!("extract-{}",&crate::digest(id.as_bytes())[..32]);
        let fields:Vec<_>=["path","destination"].iter().filter(|key|item["inputSchema"]["properties"].get(**key).is_some()).map(|key|json!({"pointer":format!("/{key}"),"kind":"FILE","multiple":false})).collect();
        let resources=if id=="web.search" {vec![json!({"kind":"URL","locator":"https://api.search.brave.com/res/v1/web/search"}),json!({"kind":"CUSTOM","locator":"secret://web-search"})]}
            else if !fields.is_empty(){vec![]}
            else if id=="client.read_log_entry"{vec![json!({"kind":"CUSTOM","locator":"client/packets/latest"})]}
            else if id.starts_with("hotfolder."){vec![json!({"kind":"FILE","locator":"hotfolder"})]}
            else{vec![json!({"kind":"CUSTOM","locator":format!("builtin/{id}")})]};
        let kinds:Vec<_>=if id=="web.search"{vec!["URL","CUSTOM"]}else if id.starts_with("hotfolder."){vec!["FILE"]}else{vec!["CUSTOM"]};
        serde_json::from_value(json!({"tool":{"id":id,"name":id,"enabled":true,"revision":1,"version":env!("CARGO_PKG_VERSION"),"packageDigest":package,"extractorId":extractor_id,
            "definition":{"id":id,"name":id,"description":item["description"],"actions":[{"name":item["action"],"resourceKinds":kinds}],"inputSchema":item["inputSchema"],"outputSchema":{"type":"object"}}},
            "extractor":{"id":extractor_id,"name":format!("Resources for {id}"),"enabled":true,"revision":1,"extractorKind":"FIELDS","version":env!("CARGO_PKG_VERSION"),"fields":fields,"fixedResources":resources,"maxResources":32}})).map_err(|_|Failure::Validation)
    }).collect()
}
pub fn builtin_info(
    item: &Value,
    profile: &InstalledAuthorizationProfile,
    package: &str,
) -> Result<BuiltinToolInfo> {
    validate(profile)?;
    let tool = &profile.tool;
    if tool.id != item["toolId"]
        || tool.version != env!("CARGO_PKG_VERSION")
        || tool.package_digest != package
        || serde_json::to_value(&tool.definition.input_schema).map_err(|_| Failure::Validation)?
            != item["inputSchema"]
    {
        return Err(Failure::Validation);
    }
    let action = item["action"].as_str().ok_or(Failure::Validation)?;
    if !tool.definition.actions.iter().any(|a| a.name == action) {
        return Err(Failure::Validation);
    }
    // A reviewed profile cannot omit a builtin's actual source/destination resources.
    let mut fields = vec![];
    if item["inputSchema"]["properties"].get("path").is_some() {
        fields.push("/path");
    }
    if item["inputSchema"]["properties"]
        .get("destination")
        .is_some()
    {
        fields.push("/destination");
    }
    if !fields.is_empty() {
        if profile.extractor.fields.len() != fields.len()
            || fields.iter().any(|p| {
                !profile
                    .extractor
                    .fields
                    .iter()
                    .any(|f| f.pointer == *p && f.kind == ResourceKind::File && !f.multiple)
            })
        {
            return Err(Failure::Validation);
        }
    } else if tool.id == "web.search" {
        if !profile.extractor.fields.is_empty()
            || ![
                ResourceDescriptor {
                    kind: ResourceKind::Url,
                    locator: "https://api.search.brave.com/res/v1/web/search".into(),
                },
                ResourceDescriptor {
                    kind: ResourceKind::Custom,
                    locator: "secret://web-search".into(),
                },
            ]
            .iter()
            .all(|r| profile.extractor.fixed_resources.contains(r))
        {
            return Err(Failure::Validation);
        }
    } else {
        let expected = if tool.id == "client.read_log_entry" {
            ResourceDescriptor {
                kind: ResourceKind::Custom,
                locator: "client/packets/latest".into(),
            }
        } else if tool.id.starts_with("hotfolder.") {
            ResourceDescriptor {
                kind: ResourceKind::File,
                locator: "hotfolder".into(),
            }
        } else {
            ResourceDescriptor {
                kind: ResourceKind::Custom,
                locator: format!("builtin/{}", tool.id),
            }
        };
        if !profile.extractor.fixed_resources.contains(&expected)
            || !profile.extractor.fields.is_empty()
        {
            return Err(Failure::Validation);
        }
    }
    Ok(BuiltinToolInfo {
        tool_id: tool.id.clone(),
        action: action.into(),
        description: item["description"]
            .as_str()
            .ok_or(Failure::Validation)?
            .into(),
        enabled: true,
        input_schema: tool.definition.input_schema.clone(),
        tool_digest: tool_digest(profile)?,
        package_digest: package.into(),
    })
}
pub fn managed_package_digest(
    tool: &LocalToolRegistration,
    runtime: &ManagedRuntime,
) -> Result<String> {
    let mut registration = serde_json::to_value(tool).map_err(|_| Failure::Validation)?;
    registration
        .as_object_mut()
        .ok_or(Failure::Validation)?
        .remove("authorizationProfile");
    Ok(crate::digest(
        &serde_json::to_vec(&json!({"registration":registration,"runtime":runtime}))
            .map_err(|_| Failure::Validation)?,
    ))
}
pub fn managed_info(
    tool: &LocalToolRegistration,
    runtime: &ManagedRuntime,
) -> Result<BuiltinToolInfo> {
    let profile = &tool.authorization_profile;
    validate(profile)?;
    if profile.tool.id != tool.tool_id
        || profile.tool.definition.input_schema != tool.input_schema
        || profile.tool.definition.output_schema != tool.output_schema
        || profile.tool.package_digest != managed_package_digest(tool, runtime)?
        || !profile
            .tool
            .definition
            .actions
            .iter()
            .any(|a| a.name == tool.action)
    {
        return Err(Failure::Validation);
    }
    Ok(BuiltinToolInfo {
        tool_id: tool.tool_id.clone(),
        action: tool.action.clone(),
        description: profile.tool.definition.description.clone(),
        enabled: true,
        input_schema: tool.input_schema.clone(),
        tool_digest: tool_digest(profile)?,
        package_digest: profile.tool.package_digest.clone(),
    })
}
