// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
//! Fixed in-sandbox source runners. Code travels as hex-framed stdin data, never host command text.
use crate::{Failure, Result};
use olo_toolgate_contracts::{LocalRuntimeKind as Kind, LocalRuntimeLimits, LocalToolRegistration};
fn hex(bytes: &[u8]) -> String {
    bytes.iter().map(|byte| format!("{byte:02x}")).collect()
}

const PYTHON: &str = "import sys,json\nsource=bytes.fromhex(sys.stdin.buffer.readline().decode().strip()).decode('utf-8')\np=json.loads(sys.stdin.buffer.read())\nscope={'__name__':'toolgate_author'}\nexec(compile(source,'<tool>','exec'),scope)\nout=scope['tool'](p['arguments'])\nprint(json.dumps({'protocolVersion':1,'requestId':p['requestId'],'output':out},allow_nan=False,separators=(',',':')))";
const NODE: &str = "const fs=require('node:fs'),vm=require('node:vm');const b=fs.readFileSync(0),i=b.indexOf(10);const source=Buffer.from(b.subarray(0,i).toString(),'hex').toString('utf8');const p=JSON.parse(b.subarray(i+1));vm.runInThisContext(source,{filename:'toolgate_author'});Promise.resolve(globalThis.tool(p.arguments)).then(output=>process.stdout.write(JSON.stringify({protocolVersion:1,requestId:p.requestId,output})));";
const POWERSHELL: &str = "$ErrorActionPreference='Stop';$hex=[Console]::ReadLine();$bytes=[byte[]]::new($hex.Length/2);for($i=0;$i -lt $bytes.Length;$i++){$bytes[$i]=[Convert]::ToByte($hex.Substring($i*2,2),16)};$source=[Text.Encoding]::UTF8.GetString($bytes);$p=[Console]::In.ReadToEnd()|ConvertFrom-Json -AsHashtable;. ([scriptblock]::Create($source));$output=tool $p.arguments;@{protocolVersion=1;requestId=$p.requestId;output=$output}|ConvertTo-Json -Compress -Depth 16";
const SHELL: &str = "IFS= read -r hex; escaped=''; for ((i=0;i<${#hex};i+=2)); do escaped+=\"\\\\x${hex:i:2}\"; done; printf -v source '%b' \"$escaped\"; eval \"$source\"; IFS= read -r invocation; tool \"$invocation\"";

/// Source is immutable signed registration data, not a user-supplied invocation field.
pub fn validate(kind: &Kind, tool: &LocalToolRegistration) -> Result<()> {
    if let Some(source) = &tool.source {
        if !matches!(
            kind,
            Kind::Python | Kind::Node | Kind::Powershell | Kind::Shell
        ) {
            return Err(Failure::Unsupported);
        }
        if source.code.is_empty() || source.code.len() > 8192 || source.code.contains('\0') {
            return Err(Failure::Validation);
        }
        let hash =
            hex(ring::digest::digest(&ring::digest::SHA256, source.code.as_bytes()).as_ref());
        if hash != source.sha256 || tool.limits.max_input_bytes > 8192 {
            return Err(Failure::Validation);
        }
    }
    Ok(())
}
pub fn command(kind: &Kind) -> Result<Vec<String>> {
    let args: Vec<&str> = match kind {
        Kind::Python => vec!["/usr/local/bin/python3", "-I", "-B", "-c", PYTHON],
        Kind::Node => vec![
            "/usr/local/bin/node",
            "--no-addons",
            "--no-warnings",
            "--eval",
            NODE,
        ],
        Kind::Powershell => vec![
            "/usr/bin/pwsh",
            "-NoLogo",
            "-NoProfile",
            "-NonInteractive",
            "-Command",
            POWERSHELL,
        ],
        Kind::Shell => vec!["/bin/bash", "--noprofile", "--norc", "-c", SHELL],
        _ => return Err(Failure::Unsupported),
    };
    let mut command = super::engine::strings(&[
        "/usr/bin/env",
        "-i",
        "PATH=/usr/local/bin:/usr/bin:/bin",
        "HOME=/work",
        "TMPDIR=/tmp",
        "LANG=C.UTF-8",
        "TZ=UTC",
        "DOTNET_EnableDiagnostics=0",
        "DOTNET_PROCESSOR_COUNT=2",
    ]);
    command.extend(args.into_iter().map(String::from));
    Ok(command)
}
/// Preserve the caller-input budget; account for the separately bounded signed source transport.
pub fn input(
    tool: &LocalToolRegistration,
    invocation: &[u8],
) -> Result<(Vec<u8>, LocalRuntimeLimits)> {
    let mut limits = tool.limits.clone();
    let mut bytes = Vec::new();
    if let Some(source) = &tool.source {
        bytes.extend(hex(source.code.as_bytes()).bytes());
        bytes.push(b'\n');
        limits.max_input_bytes = limits
            .max_input_bytes
            .checked_add(16385)
            .ok_or(Failure::Validation)?;
    }
    bytes.extend(invocation);
    bytes.push(b'\n');
    // The terminating transport newline is not part of the caller's JSON budget.
    limits.max_input_bytes = limits
        .max_input_bytes
        .checked_add(1)
        .ok_or(Failure::Validation)?;
    Ok((bytes, limits))
}
