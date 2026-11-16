// Copyright 2026 OLO Labs; SPDX-License-Identifier: Apache-2.0
//! Trusted ingress authentication only (ADR 067). This library exposes no HTTP listener,
//! lease or admission API. Authentication is not durable acceptance or replay protection.
//! Packages never receive the broker's signing key material.

use ring::{digest, hmac};
use std::fmt;

mod http_request;

pub const MAX_BODY_BYTES: usize = 16_384;
pub const MAX_FRESHNESS_SECONDS: u64 = 300;
const MAX_TIMESTAMP: u64 = 9_007_199_254_740_991;
const DOMAIN: &[u8] = b"olo.trigger-ingress.v1\n";

#[derive(Clone, Copy, Debug, PartialEq, Eq)]
pub enum Refusal {
    InvalidPolicy,
    InvalidScope,
    MalformedRequest,
    MethodNotAllowed,
    UnsupportedMediaType,
    BodyTooLarge,
    NotFresh,
    Unauthenticated,
}

impl fmt::Display for Refusal {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        // No key material, raw headers or source content in diagnostics.
        write!(f, "trigger ingress request refused: {self:?}")
    }
}
impl std::error::Error for Refusal {}

/// Trusted profile/broker limits. Private fields prevent unchecked policy construction.
pub struct Policy {
    max_body_bytes: usize,
    freshness_seconds: u64,
}
impl Policy {
    pub fn new(max_body_bytes: usize, freshness_seconds: u64) -> Result<Self, Refusal> {
        if !(1..=MAX_BODY_BYTES).contains(&max_body_bytes)
            || !(1..=MAX_FRESHNESS_SECONDS).contains(&freshness_seconds)
        {
            return Err(Refusal::InvalidPolicy);
        }
        Ok(Self {
            max_body_bytes,
            freshness_seconds,
        })
    }
}

/// Broker-resolved tenant/binding, never taken as authority from request content.
pub struct Scope<'a> {
    tenant: &'a str,
    binding: &'a str,
}
impl<'a> Scope<'a> {
    pub fn new(tenant: &'a str, binding: &'a str) -> Result<Self, Refusal> {
        if !valid_id(tenant) || !valid_id(binding) {
            return Err(Refusal::InvalidScope);
        }
        Ok(Self { tenant, binding })
    }
}

/// Raw fields after an HTTP adapter has rejected duplicate/ambiguous headers.
/// No diagnostic formatting is provided because body and header values may be sensitive.
pub struct Request<'a> {
    pub delivery_id: &'a str,
    pub timestamp: &'a str,
    pub signature: &'a str,
    pub body: &'a [u8],
}

/// A cryptographic result only. The caller still must validate current authority,
/// lease, JSON/event schema and durable replay/admission before acknowledging a sender.
pub struct Authenticated<'a> {
    tenant: &'a str,
    binding: &'a str,
    delivery_id: &'a str,
    signed_at: u64,
    body: &'a [u8],
    body_digest: [u8; 32],
}
impl<'a> Authenticated<'a> {
    pub fn tenant(&self) -> &'a str {
        self.tenant
    }
    pub fn binding(&self) -> &'a str {
        self.binding
    }
    pub fn delivery_id(&self) -> &'a str {
        self.delivery_id
    }
    pub fn signed_at(&self) -> u64 {
        self.signed_at
    }
    pub fn body(&self) -> &'a [u8] {
        self.body
    }
    pub fn body_digest(&self) -> &[u8; 32] {
        &self.body_digest
    }
}

/// Lives exclusively in trusted broker custody. No Debug/Display/serialization of keys.
pub struct Verifier {
    policy: Policy,
    keys: Vec<hmac::Key>,
}
impl Verifier {
    pub fn new(policy: Policy, keys: &[&[u8]]) -> Result<Self, Refusal> {
        if !(1..=2).contains(&keys.len()) || keys.iter().any(|key| !(32..=64).contains(&key.len()))
        {
            return Err(Refusal::InvalidPolicy);
        }
        Ok(Self {
            policy,
            keys: keys
                .iter()
                .map(|key| hmac::Key::new(hmac::HMAC_SHA256, key))
                .collect(),
        })
    }

    pub fn authenticate<'a>(
        &self,
        scope: Scope<'a>,
        request: Request<'a>,
        now_seconds: u64,
    ) -> Result<Authenticated<'a>, Refusal> {
        if request.body.len() > self.policy.max_body_bytes {
            return Err(Refusal::BodyTooLarge);
        }
        if !valid_id(request.delivery_id)
            || request.timestamp.is_empty()
            || request.timestamp.len() > 16
            || !request.timestamp.bytes().all(|byte| byte.is_ascii_digit())
            || request.timestamp.len() > 1 && request.timestamp.starts_with('0')
        {
            return Err(Refusal::MalformedRequest);
        }
        let signed_at = request
            .timestamp
            .parse::<u64>()
            .map_err(|_| Refusal::MalformedRequest)?;
        if signed_at > MAX_TIMESTAMP || now_seconds > MAX_TIMESTAMP {
            return Err(Refusal::MalformedRequest);
        }
        if now_seconds.abs_diff(signed_at) > self.policy.freshness_seconds {
            return Err(Refusal::NotFresh);
        }
        let tag = decode_tag(request.signature)?;
        // Every prefix field is bounded ASCII without LF. The body is authenticated unchanged.
        let mut message = Vec::with_capacity(DOMAIN.len() + 3 * 129 + 17 + request.body.len());
        message.extend_from_slice(DOMAIN);
        for field in [
            scope.tenant,
            scope.binding,
            request.delivery_id,
            request.timestamp,
        ] {
            message.extend_from_slice(field.as_bytes());
            message.push(b'\n');
        }
        message.extend_from_slice(request.body);
        // Do not short-circuit rotation-key verification. ring verifies tags in constant time.
        let mut verified = false;
        for key in &self.keys {
            verified |= hmac::verify(key, &message, &tag).is_ok();
        }
        if !verified {
            return Err(Refusal::Unauthenticated);
        }
        let mut body_digest = [0_u8; 32];
        body_digest.copy_from_slice(digest::digest(&digest::SHA256, request.body).as_ref());
        Ok(Authenticated {
            tenant: scope.tenant,
            binding: scope.binding,
            delivery_id: request.delivery_id,
            signed_at,
            body: request.body,
            body_digest,
        })
    }
}

fn valid_id(value: &str) -> bool {
    (1..=128).contains(&value.len())
        && value
            .bytes()
            .all(|byte| byte.is_ascii_alphanumeric() || b"._-".contains(&byte))
}

fn decode_tag(value: &str) -> Result<[u8; 32], Refusal> {
    if value.len() != 67 || !value.starts_with("v1=") {
        return Err(Refusal::MalformedRequest);
    }
    let nibble = |byte: u8| match byte {
        b'0'..=b'9' => Ok(byte - b'0'),
        b'a'..=b'f' => Ok(byte - b'a' + 10),
        _ => Err(Refusal::MalformedRequest),
    };
    let mut decoded = [0_u8; 32];
    for (index, pair) in value.as_bytes()[3..].chunks_exact(2).enumerate() {
        decoded[index] = nibble(pair[0])? * 16 + nibble(pair[1])?;
    }
    Ok(decoded)
}
