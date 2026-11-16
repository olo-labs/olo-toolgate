// Copyright 2026 OLO Labs; SPDX-License-Identifier: Apache-2.0
//! HTTP envelope checks after the server's bounded HTTP decoder, before authentication.
use crate::{Authenticated, Refusal, Request, Scope, Verifier};
use http::{HeaderMap, Method};

const MAX_HEADERS: usize = 32;
const MAX_HEADER_BYTES: usize = 8192;

impl Verifier {
    /// Authenticates a decoded HTTP POST envelope, not JSON/event schemas or admission.
    /// The body is after HTTP transfer decoding, before any content decoding/normalization.
    /// The server must enforce streaming/header limits before buffering the request.
    pub fn authenticate_http<'a>(
        &self,
        scope: Scope<'a>,
        method: &Method,
        headers: &'a HeaderMap,
        body: &'a [u8],
        now_seconds: u64,
    ) -> Result<Authenticated<'a>, Refusal> {
        if method != Method::POST {
            return Err(Refusal::MethodNotAllowed);
        }
        if body.len() > self.policy.max_body_bytes {
            return Err(Refusal::BodyTooLarge);
        }
        if headers.len() > MAX_HEADERS {
            return Err(Refusal::MalformedRequest);
        }
        let mut total = 0;
        for (name, value) in headers {
            if name.as_str().len() > 64 || value.as_bytes().len() > 1024 {
                return Err(Refusal::MalformedRequest);
            }
            total += name.as_str().len() + value.as_bytes().len();
        }
        if total > MAX_HEADER_BYTES {
            return Err(Refusal::MalformedRequest);
        }
        one(headers, "host")?;

        let content_type = one(headers, "content-type")?.ok_or(Refusal::UnsupportedMediaType)?;
        json_content_type(content_type)?;
        if one(headers, "content-encoding")?
            .is_some_and(|value| !value.trim_ascii().eq_ignore_ascii_case("identity"))
        {
            return Err(Refusal::UnsupportedMediaType);
        }
        let length = one(headers, "content-length")?;
        let transfer = one(headers, "transfer-encoding")?;
        if length.is_some() && transfer.is_some() {
            return Err(Refusal::MalformedRequest);
        }
        if let Some(length) = length {
            if length.is_empty()
                || length.len() > 5
                || !length.bytes().all(|byte| byte.is_ascii_digit())
                || length.len() > 1 && length.starts_with('0')
                || length.parse::<usize>().ok() != Some(body.len())
            {
                return Err(Refusal::MalformedRequest);
            }
        }
        if transfer.is_some_and(|value| !value.trim_ascii().eq_ignore_ascii_case("chunked")) {
            return Err(Refusal::MalformedRequest);
        }

        self.authenticate(
            scope,
            Request {
                delivery_id: required(headers, "olo-delivery-id")?,
                timestamp: required(headers, "olo-timestamp")?,
                signature: required(headers, "olo-signature")?,
                body,
            },
            now_seconds,
        )
    }
}

/// Authentication/framing fields must have exactly one value, case-insensitively.
fn one<'a>(headers: &'a HeaderMap, name: &str) -> Result<Option<&'a str>, Refusal> {
    let mut values = headers.get_all(name).iter();
    let first = values.next();
    if values.next().is_some() {
        return Err(Refusal::MalformedRequest);
    }
    first
        .map(|value| value.to_str().map_err(|_| Refusal::MalformedRequest))
        .transpose()
}

fn required<'a>(headers: &'a HeaderMap, name: &str) -> Result<&'a str, Refusal> {
    one(headers, name)?.ok_or(Refusal::MalformedRequest)
}

fn json_content_type(value: &str) -> Result<(), Refusal> {
    let mut parts = value.split(';');
    if !parts
        .next()
        .is_some_and(|kind| kind.trim_ascii().eq_ignore_ascii_case("application/json"))
    {
        return Err(Refusal::UnsupportedMediaType);
    }
    let mut charset = false;
    for parameter in parts {
        let Some((name, raw)) = parameter.split_once('=') else {
            return Err(Refusal::UnsupportedMediaType);
        };
        let raw = raw.trim_ascii();
        let encoding = raw
            .strip_prefix('"')
            .and_then(|quoted| quoted.strip_suffix('"'))
            .unwrap_or(raw);
        if charset
            || !name.trim_ascii().eq_ignore_ascii_case("charset")
            || !encoding.eq_ignore_ascii_case("utf-8")
        {
            return Err(Refusal::UnsupportedMediaType);
        }
        charset = true;
    }
    Ok(())
}
