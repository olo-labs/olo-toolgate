// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
//! Fixed-path bounded HTTPS transport; identity/private material never enters error strings.
use crate::{config::Config, contracts::Contracts, identity::DeviceKey, Failure, Result};
use olo_toolgate_contracts::*;
use std::{future::Future, pin::Pin, sync::Arc, time::Duration};
pub type Call<'a, T> = Pin<Box<dyn Future<Output = Result<T>> + Send + 'a>>;
pub trait ControlPort: Send + Sync {
    fn desired(&self, _identity: DeviceIdentity) -> Call<'_, FleetSignedDocument> {
        Box::pin(async { Err(Failure::Unsupported) })
    }
    fn artifact_grant(
        &self,
        _identity: DeviceIdentity,
        _request: FleetArtifactGrantRequest,
    ) -> Call<'_, FleetSignedDocument> {
        Box::pin(async { Err(Failure::Unsupported) })
    }
    fn artifact(
        &self,
        _identity: DeviceIdentity,
        _grant: FleetSignedDocument,
    ) -> Call<'_, Vec<u8>> {
        Box::pin(async { Err(Failure::Unsupported) })
    }
    fn discovery(&self) -> Call<'_, SignedClientDiscovery>;
    fn start(&self, request: EndpointEnrollmentStart) -> Call<'_, EndpointEnrollmentChallenge>;
    fn poll(&self, request: EndpointEnrollmentPoll) -> Call<'_, EndpointEnrollmentResult>;
    fn check_in(
        &self,
        identity: DeviceIdentity,
        request: EndpointCheckIn,
    ) -> Call<'_, EndpointCheckInAck>;
}
pub struct HttpsControl {
    origin: String,
    client: reqwest::Client,
    config: Config,
    key: Arc<DeviceKey>,
    contracts: Arc<Contracts>,
}
impl HttpsControl {
    pub fn new(config: Config, key: Arc<DeviceKey>, contracts: Arc<Contracts>) -> Result<Self> {
        config.validate()?;
        let client = Self::builder(&config)?
            .build()
            .map_err(|_| Failure::Unavailable)?;
        Ok(Self {
            origin: crate::config::origin(&config.server_url)?,
            client,
            config,
            key,
            contracts,
        })
    }
    fn builder(config: &Config) -> Result<reqwest::ClientBuilder> {
        let _ = rustls::crypto::ring::default_provider().install_default();
        let mut builder = reqwest::Client::builder()
            .https_only(true)
            .redirect(reqwest::redirect::Policy::none())
            .no_proxy()
            .tls_sslkeylogfile(false)
            .referer(false)
            .retry(reqwest::retry::never())
            .timeout(Duration::from_secs(config.request_timeout_seconds))
            .connect_timeout(Duration::from_secs(5))
            .pool_max_idle_per_host(1)
            .user_agent(concat!("olo-toolgate-client/", env!("CARGO_PKG_VERSION")));
        if let Some(path) = &config.ca_certificate_path {
            let bytes = crate::storage::read_owned(path, 16384, false)?;
            let root = reqwest::Certificate::from_pem(&bytes).map_err(|_| Failure::Validation)?;
            builder = builder.add_root_certificate(root);
        }
        Ok(builder)
    }
    async fn request<T: serde::de::DeserializeOwned>(
        &self,
        client: &reqwest::Client,
        path: &str,
        body: Option<Vec<u8>>,
        model: &str,
    ) -> Result<T> {
        let url = format!("{}{path}", self.origin);
        let correlation = crate::identity::nonce()?;
        let mut request = if body.is_some() {
            client.post(url)
        } else {
            client.get(url)
        };
        request = request
            .header("Accept", "application/json")
            .header("X-Request-ID", correlation);
        let mut response = if let Some(body) = body {
            request
                .header("Content-Type", "application/json")
                .body(body)
        } else {
            request
        }
        .send()
        .await
        .map_err(|_| Failure::Unavailable)?;
        match response.status().as_u16() {
            200 => {}
            401 | 403 => return Err(Failure::Revoked),
            409 => return Err(Failure::Conflict),
            _ => return Err(Failure::Unavailable),
        }
        if response.content_length().is_some_and(|n| n > 131072)
            || response
                .headers()
                .get("content-type")
                .and_then(|h| h.to_str().ok())
                .is_none_or(|v| v.split(';').next() != Some("application/json"))
        {
            return Err(Failure::Validation);
        }
        let mut bytes = Vec::new();
        while let Some(chunk) = response.chunk().await.map_err(|_| Failure::Unavailable)? {
            if bytes.len() + chunk.len() > 131072 {
                return Err(Failure::Validation);
            }
            bytes.extend_from_slice(&chunk);
        }
        self.contracts.decode(model, &bytes)
    }
}
impl ControlPort for HttpsControl {
    fn desired(&self, identity: DeviceIdentity) -> Call<'_, FleetSignedDocument> {
        Box::pin(async move {
            let client = Self::builder(&self.config)?
                .identity(self.key.tls_identity(&identity)?)
                .build()
                .map_err(|_| Failure::Unavailable)?;
            self.request(
                &client,
                "/api/control/v1/fleet/desired",
                None,
                "FleetSignedDocument",
            )
            .await
        })
    }
    fn artifact_grant(
        &self,
        identity: DeviceIdentity,
        request: FleetArtifactGrantRequest,
    ) -> Call<'_, FleetSignedDocument> {
        Box::pin(async move {
            let client = Self::builder(&self.config)?
                .identity(self.key.tls_identity(&identity)?)
                .build()
                .map_err(|_| Failure::Unavailable)?;
            self.request(
                &client,
                "/api/control/v1/fleet/artifact-grants",
                Some(
                    self.contracts
                        .encode("FleetArtifactGrantRequest", &request)?,
                ),
                "FleetSignedDocument",
            )
            .await
        })
    }
    fn artifact(&self, identity: DeviceIdentity, grant: FleetSignedDocument) -> Call<'_, Vec<u8>> {
        Box::pin(async move {
            let client = Self::builder(&self.config)?
                .identity(self.key.tls_identity(&identity)?)
                .build()
                .map_err(|_| Failure::Unavailable)?;
            let mut response = client
                .post(format!(
                    "{}/api/control/v1/fleet/artifacts/download",
                    self.origin
                ))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .header("X-Request-ID", crate::identity::nonce()?)
                .body(self.contracts.encode("FleetSignedDocument", &grant)?)
                .send()
                .await
                .map_err(|_| Failure::Unavailable)?;
            match response.status().as_u16() {
                200 => {}
                401 | 403 => return Err(Failure::Revoked),
                409 => return Err(Failure::Conflict),
                _ => return Err(Failure::Unavailable),
            }
            if response.content_length().is_some_and(|n| n > 32768)
                || response
                    .headers()
                    .get("content-type")
                    .and_then(|h| h.to_str().ok())
                    .is_none_or(|v| v.split(';').next() != Some("application/json"))
            {
                return Err(Failure::Validation);
            }
            let mut bytes = Vec::new();
            while let Some(chunk) = response.chunk().await.map_err(|_| Failure::Unavailable)? {
                if bytes.len() + chunk.len() > 32768 {
                    return Err(Failure::Validation);
                }
                bytes.extend_from_slice(&chunk);
            }
            Ok(bytes)
        })
    }
    fn discovery(&self) -> Call<'_, SignedClientDiscovery> {
        Box::pin(self.request(
            &self.client,
            "/.well-known/olo-toolgate-client",
            None,
            "SignedClientDiscovery",
        ))
    }
    fn start(&self, request: EndpointEnrollmentStart) -> Call<'_, EndpointEnrollmentChallenge> {
        Box::pin(async move {
            self.request(
                &self.client,
                "/api/control/v1/endpoint/enrollments",
                Some(self.contracts.encode("EndpointEnrollmentStart", &request)?),
                "EndpointEnrollmentChallenge",
            )
            .await
        })
    }
    fn poll(&self, request: EndpointEnrollmentPoll) -> Call<'_, EndpointEnrollmentResult> {
        Box::pin(async move {
            self.request(
                &self.client,
                "/api/control/v1/endpoint/enrollments/poll",
                Some(self.contracts.encode("EndpointEnrollmentPoll", &request)?),
                "EndpointEnrollmentResult",
            )
            .await
        })
    }
    fn check_in(
        &self,
        identity: DeviceIdentity,
        request: EndpointCheckIn,
    ) -> Call<'_, EndpointCheckInAck> {
        Box::pin(async move {
            let client = Self::builder(&self.config)?
                .identity(self.key.tls_identity(&identity)?)
                .build()
                .map_err(|_| Failure::Unavailable)?;
            self.request(
                &client,
                "/api/control/v1/endpoint/check-in",
                Some(self.contracts.encode("EndpointCheckIn", &request)?),
                "EndpointCheckInAck",
            )
            .await
        })
    }
}
