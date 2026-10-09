// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
//! Fixed tools, bounded input/output and a fresh Gateway grant before every protected operation.
use crate::{
    hotfolder::{HotFolder, Settings as FolderSettings},
    transport::Call,
    Failure, Result,
};
use olo_toolgate_contracts::*;
use serde::{Deserialize, Serialize};
use serde_json::{json, Value};
use std::{
    collections::{BTreeMap, VecDeque},
    path::PathBuf,
    sync::Arc,
};

#[derive(Clone, Deserialize, Serialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
pub struct Settings {
    pub hotfolder: FolderSettings,
    pub gateway_url: String,
    pub gateway_token_path: PathBuf,
    pub gateway_ca_path: Option<PathBuf>,
    pub device_id: String,
    #[serde(default)]
    pub authorization_profiles: Vec<InstalledAuthorizationProfile>,
    pub web_search_token_path: Option<PathBuf>,
    #[serde(default)]
    pub web_search_allowed_domains: Vec<String>,
}
impl Settings {
    pub fn validate(&self) -> Result<()> {
        if self.authorization_profiles.len() > 64
            || self.web_search_allowed_domains.len() > 16
            || self.web_search_allowed_domains.iter().any(|host| {
                host.len() > 253
                    || !host.contains('.')
                    || host.starts_with('.')
                    || host.ends_with('.')
                    || !host.bytes().all(|b| {
                        b.is_ascii_lowercase() || b.is_ascii_digit() || b == b'.' || b == b'-'
                    })
            })
        {
            return Err(Failure::Validation);
        }
        self.hotfolder.validate()?;
        crate::config::origin(&self.gateway_url)?;
        if !self.gateway_token_path.is_absolute()
            || self.device_id.is_empty()
            || self.device_id.len() > 128
            || self
                .gateway_ca_path
                .as_ref()
                .is_some_and(|p| !p.is_absolute())
            || self
                .web_search_token_path
                .as_ref()
                .is_some_and(|p| !p.is_absolute())
        {
            return Err(Failure::Validation);
        }
        Ok(())
    }
}

/// The adapter must return only after validating/consuming a grant for this exact request.
pub trait AuthorizationPort: Send + Sync {
    fn authorize(&self, request: AuthorizationRequest) -> Call<'_, ()>;
    /// Managed execution needs the consumed permit's deadline. An adapter that
    /// cannot provide bounded authorization is unsupported, never implicitly ALLOW.
    fn authorize_bound(&self, _request: AuthorizationRequest) -> Call<'_, Option<u64>> {
        Box::pin(async { Err(Failure::Unsupported) })
    }
    fn complete(
        &self,
        _request: AuthorizationRequest,
        _output: BTreeMap<String, Value>,
    ) -> Call<'_, ()> {
        Box::pin(async { Ok(()) })
    }
}

pub struct Executor {
    folder: HotFolder,
    authorization: Arc<dyn AuthorizationPort>,
    catalog: Vec<BuiltinToolInfo>,
    validators: BTreeMap<String, jsonschema::Validator>,
    events: VecDeque<Value>,
    sequence: u64,
    web: Option<(reqwest::Client, String)>,
    web_domains: Vec<String>,
    epoch: String,
    packet_log: Option<crate::diagnostics::PacketLog>,
}
impl Executor {
    pub fn new(settings: &Settings, authorization: Arc<dyn AuthorizationPort>) -> Result<Self> {
        settings.validate()?;
        let data: Value = serde_json::from_str(include_str!(
            "../../../packages/contracts/tools/builtins.json"
        ))
        .map_err(|_| Failure::Validation)?;
        let mut catalog = Vec::new();
        let mut validators = BTreeMap::new();
        let package = crate::authorization_profile::builtin_package_digest()?;
        for item in data["tools"].as_array().ok_or(Failure::Validation)? {
            let matches: Vec<_> = settings
                .authorization_profiles
                .iter()
                .filter(|p| p.tool.id == item["toolId"])
                .collect();
            if matches.is_empty() {
                continue;
            }
            if matches.len() != 1 {
                return Err(Failure::Validation);
            }
            let mut tool = crate::authorization_profile::builtin_info(item, matches[0], &package)?;
            tool.enabled = tool.tool_id != "client.read_log_entry"
                && (tool.tool_id != "web.search" || settings.web_search_token_path.is_some());
            validators.insert(
                tool.tool_id.clone(),
                jsonschema::validator_for(
                    &serde_json::to_value(&tool.input_schema).map_err(|_| Failure::Validation)?,
                )
                .map_err(|_| Failure::Validation)?,
            );
            catalog.push(tool);
        }
        let web = if let Some(path) = &settings.web_search_token_path {
            Some((
                super::tool_gateway::client(None)?,
                super::tool_gateway::secret(path)?,
            ))
        } else {
            None
        };
        Ok(Self {
            web_domains: settings.web_search_allowed_domains.clone(),
            epoch: crate::identity::nonce()?,
            folder: HotFolder::open(settings.hotfolder.clone())?,
            authorization,
            catalog,
            validators,
            events: VecDeque::new(),
            sequence: 0,
            web,
            packet_log: None,
        })
    }
    pub fn with_packet_log(mut self, log: crate::diagnostics::PacketLog) -> Self {
        self.packet_log = Some(log);
        if let Some(tool) = self
            .catalog
            .iter_mut()
            .find(|tool| tool.tool_id == "client.read_log_entry")
        {
            tool.enabled = true;
        }
        self
    }
    pub fn catalog(&self) -> Vec<BuiltinToolInfo> {
        self.catalog.clone()
    }
    pub async fn execute(
        &mut self,
        invocation: BuiltinInvocation,
        valid_until: u64,
    ) -> Result<BTreeMap<String, Value>> {
        let deadline = std::time::Instant::now()
            + std::time::Duration::from_millis(valid_until.saturating_sub(crate::now()).min(20000));
        if valid_until <= crate::now() {
            return Err(Failure::Expired);
        }
        let tool = self
            .catalog
            .iter()
            .find(|t| t.tool_id == invocation.tool_id && t.enabled)
            .ok_or(Failure::Unsupported)?
            .clone();
        let args = serde_json::to_value(&invocation.arguments).map_err(|_| Failure::Validation)?;
        if !self.validators[&tool.tool_id].is_valid(&args) {
            return Err(Failure::Validation);
        }
        let path = invocation
            .arguments
            .get("path")
            .and_then(Value::as_str)
            .unwrap_or("hotfolder");
        if tool.tool_id.starts_with("hotfolder.") && invocation.arguments.contains_key("path") {
            crate::hotfolder::components(path)?;
        }
        if let Some(destination) = args.get("destination").and_then(Value::as_str) {
            crate::hotfolder::components(destination)?;
        }
        let request = AuthorizationRequest {
            tool_id: tool.tool_id.clone(),
            action: tool.action.clone(),
            arguments: invocation.arguments.clone(),
        };
        self.authorization.authorize(request.clone()).await?;
        let string = |name: &str| args[name].as_str().ok_or(Failure::Validation);
        if std::time::Instant::now() >= deadline {
            return Err(Failure::Expired);
        }
        let result = match tool.tool_id.as_str() {
            "client.read_log_entry" => {
                json!({"file":"packets.jsonl","entry":self.packet_log.as_ref().ok_or(Failure::Unsupported)?.latest()?})
            }
            "hotfolder.list" => json!({"paths":self.folder.list()?}),
            "hotfolder.read_text" => {
                json!({"text":String::from_utf8(self.folder.read(path)?).map_err(|_|Failure::Validation)?})
            }
            "hotfolder.hash" => json!({"sha256":crate::digest(&self.folder.read(path)?)}),
            "hotfolder.file_info" => self.folder.info(path)?,
            "hotfolder.write_text" | "hotfolder.append_text" => {
                self.folder
                    .write(path, string("text")?.as_bytes(), tool.action == "append")?;
                self.event(path, "WRITE");
                json!({"success":true})
            }
            "hotfolder.mkdir" => {
                self.folder.mkdir(path)?;
                self.event(path, "MKDIR");
                json!({"success":true})
            }
            "hotfolder.move" | "hotfolder.copy" => {
                let destination = string("destination")?;
                self.folder
                    .transfer(path, destination, tool.action == "move")?;
                self.event(
                    path,
                    if tool.action == "move" {
                        "MOVE"
                    } else {
                        "COPY"
                    },
                );
                self.event(destination, "WRITE");
                json!({"success":true})
            }
            "hotfolder.search_text" => {
                let content =
                    String::from_utf8(self.folder.read(path)?).map_err(|_| Failure::Validation)?;
                let matches: Vec<_> = content
                    .lines()
                    .enumerate()
                    .filter(|(_, line)| line.contains(string("query").unwrap_or("")))
                    .take(100)
                    .map(|(line, _)| line + 1)
                    .collect();
                json!({"lines":matches})
            }
            "hotfolder.watch_events" | "hotfolder.list_events" => {
                let after = args["after"].as_u64().ok_or(Failure::Validation)?;
                let reset = args
                    .get("epoch")
                    .and_then(Value::as_str)
                    .is_some_and(|epoch| epoch != self.epoch)
                    || after > self.sequence
                    || self.events.front().is_some_and(|e| {
                        after.saturating_add(1) < e["sequence"].as_u64().unwrap_or(0)
                    });
                let events: Vec<_> = self
                    .events
                    .iter()
                    .filter(|e| e["sequence"].as_u64().is_some_and(|s| reset || s > after))
                    .take(100)
                    .collect();
                let cursor = events
                    .last()
                    .and_then(|e| e["sequence"].as_u64())
                    .unwrap_or(after.min(self.sequence));
                json!({"events":events,"sequence":cursor,"latestSequence":self.sequence,"epoch":self.epoch,"reset":reset})
            }
            "hash.sha256" => json!({"sha256":crate::digest(string("text")?.as_bytes())}),
            "text.transform" => {
                json!({"text":match string("mode")? {"upper"=>string("text")?.to_uppercase(),"lower"=>string("text")?.to_lowercase(),"trim"=>string("text")?.trim().to_owned(),_=>return Err(Failure::Validation)}})
            }
            "json.validate" => {
                let _: Value =
                    serde_json::from_str(string("text")?).map_err(|_| Failure::Validation)?;
                json!({"valid":true})
            }
            "calculator.evaluate" => json!({"value":calculate(string("expression")?)?}),
            "system.info" => {
                json!({"os":std::env::consts::OS,"architecture":std::env::consts::ARCH,"version":env!("CARGO_PKG_VERSION")})
            }
            "web.search" => self.search(string("query")?).await?,
            _ => return Err(Failure::Unsupported),
        };
        if serde_json::to_vec(&result)
            .map_err(|_| Failure::Validation)?
            .len()
            > 120000
        {
            return Err(Failure::Validation);
        }
        tracing::info!(event="builtin_execution",tool_id=%tool.tool_id,arguments_digest=%crate::digest(&serde_json::to_vec(&request.arguments).map_err(|_|Failure::Validation)?),result="success");
        let output: BTreeMap<String, Value> =
            serde_json::from_value(result).map_err(|_| Failure::Validation)?;
        self.authorization.complete(request, output.clone()).await?;
        Ok(output)
    }
    fn event(&mut self, path: &str, kind: &str) {
        self.sequence = self.sequence.saturating_add(1);
        if self.events.len() == 256 {
            self.events.pop_front();
        }
        self.events.push_back(json!({"sequence":self.sequence,"path":path,"kind":kind,"timestampUnixMs":crate::now()}));
    }
    async fn search(&self, query: &str) -> Result<Value> {
        let (client, token) = self.web.as_ref().ok_or(Failure::Unsupported)?;
        let mut url = reqwest::Url::parse("https://api.search.brave.com/res/v1/web/search")
            .map_err(|_| Failure::Validation)?;
        url.query_pairs_mut().extend_pairs([
            ("q", query),
            ("count", "5"),
            ("safesearch", "strict"),
        ]);
        let response = client
            .get(url)
            .header("X-Subscription-Token", token)
            .header("Accept", "application/json")
            .send()
            .await
            .map_err(|_| Failure::Unavailable)?;
        let bytes = super::tool_gateway::body(response).await?;
        let data: Value = serde_json::from_slice(&bytes).map_err(|_| Failure::Validation)?;
        let results: Vec<_> = data["web"]["results"]
            .as_array()
            .unwrap_or(&Vec::new())
            .iter()
            .take(5)
            .filter_map(|r| {
                let url = r["url"].as_str()?;
                let title = r["title"].as_str()?;
                if url.len() > 2048 || title.len() > 512 || !url.starts_with("https://") {
                    return None;
                }
                let parsed = reqwest::Url::parse(url).ok()?;
                let host = parsed.host_str()?;
                if !self.web_domains.is_empty()
                    && !self
                        .web_domains
                        .iter()
                        .any(|domain| host == domain || host.ends_with(&format!(".{domain}")))
                {
                    return None;
                }
                Some(json!({"url":url,"title":title}))
            })
            .collect();
        Ok(json!({"results":results}))
    }
}

/// Small arithmetic parser: no names, functions, power operator, evaluation engine or subprocess.
pub fn calculate(expression: &str) -> Result<f64> {
    struct Parser<'a> {
        bytes: &'a [u8],
        pos: usize,
    }
    impl Parser<'_> {
        fn space(&mut self) {
            while self
                .bytes
                .get(self.pos)
                .is_some_and(u8::is_ascii_whitespace)
            {
                self.pos += 1;
            }
        }
        fn expression(&mut self, depth: u32) -> Result<f64> {
            if depth > 32 {
                return Err(Failure::Validation);
            }
            let mut value = self.term(depth + 1)?;
            loop {
                self.space();
                let operator = self.bytes.get(self.pos).copied();
                if !matches!(operator, Some(b'+' | b'-')) {
                    break;
                }
                self.pos += 1;
                let rhs = self.term(depth + 1)?;
                value = if operator == Some(b'+') {
                    value + rhs
                } else {
                    value - rhs
                };
            }
            Ok(value)
        }
        fn term(&mut self, depth: u32) -> Result<f64> {
            let mut value = self.atom(depth + 1)?;
            loop {
                self.space();
                let operator = self.bytes.get(self.pos).copied();
                if !matches!(operator, Some(b'*' | b'/')) {
                    break;
                }
                self.pos += 1;
                let rhs = self.atom(depth + 1)?;
                value = if operator == Some(b'*') {
                    value * rhs
                } else {
                    value / rhs
                };
            }
            Ok(value)
        }
        fn atom(&mut self, depth: u32) -> Result<f64> {
            if depth > 32 {
                return Err(Failure::Validation);
            }
            self.space();
            if self.bytes.get(self.pos) == Some(&b'(') {
                self.pos += 1;
                let value = self.expression(depth + 1)?;
                self.space();
                if self.bytes.get(self.pos) != Some(&b')') {
                    return Err(Failure::Validation);
                }
                self.pos += 1;
                return Ok(value);
            }
            if matches!(self.bytes.get(self.pos), Some(b'+' | b'-')) {
                let negative = self.bytes[self.pos] == b'-';
                self.pos += 1;
                return self.atom(depth + 1).map(|v| if negative { -v } else { v });
            }
            let start = self.pos;
            while self
                .bytes
                .get(self.pos)
                .is_some_and(|b| b.is_ascii_digit() || *b == b'.')
            {
                self.pos += 1;
            }
            std::str::from_utf8(&self.bytes[start..self.pos])
                .map_err(|_| Failure::Validation)?
                .parse()
                .map_err(|_| Failure::Validation)
        }
    }
    if expression.len() > 1024 {
        return Err(Failure::Validation);
    }
    let mut parser = Parser {
        bytes: expression.as_bytes(),
        pos: 0,
    };
    let value = parser.expression(0)?;
    parser.space();
    if parser.pos != parser.bytes.len() || !value.is_finite() {
        return Err(Failure::Validation);
    }
    Ok(value)
}
