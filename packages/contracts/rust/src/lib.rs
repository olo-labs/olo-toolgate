//! Shared, versioned contracts for OLO ToolGate.
//!
//! This initial scaffold intentionally contains only the contract-set marker.
//! Generated models will be added by the Foundation/Contracts module.

use serde::{Deserialize, Serialize};

/// Identifies the canonical contract set used to generate this library.
#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
pub struct ContractSet {
    pub name: String,
    pub version: String,
}

impl ContractSet {
    pub fn current() -> Self {
        Self {
            name: "olo-toolgate-contracts".to_owned(),
            version: env!("CARGO_PKG_VERSION").to_owned(),
        }
    }
}
