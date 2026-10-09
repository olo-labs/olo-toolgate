// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
//! Read-only activity remains available while the execution mutex is held.
use crate::{storage::ProtectedStore, Result};
use olo_toolgate_contracts::*;
use std::sync::{
    atomic::{AtomicU64, Ordering},
    Arc, Mutex,
};

static NEXT: AtomicU64 = AtomicU64::new(1);
pub struct Activity {
    state: Mutex<ClientActivity>,
    store: ProtectedStore,
}
fn safe(value: &str) -> String {
    if !value.is_empty()
        && value.len() <= 128
        && value
            .bytes()
            .all(|b| b.is_ascii_alphanumeric() || b"._-: ".contains(&b))
    {
        value.to_owned()
    } else {
        "[redacted]".into()
    }
}
impl Activity {
    pub fn open(directory: std::path::PathBuf) -> Result<Arc<Self>> {
        let store = ProtectedStore::open(directory)?;
        let mut state: ClientActivity = store
            .read("activity.json")?
            .and_then(|bytes| serde_json::from_slice(&bytes).ok())
            .unwrap_or(ClientActivity {
                active: vec![],
                last_command: None,
                events: vec![],
                log_available: true,
            });
        // A restart cannot manufacture success for interrupted work.
        for mut command in std::mem::take(&mut state.active) {
            command.state = ClientCommandState::Interrupted;
            command.finished_at_unix_ms = Some(crate::now());
            state.events.push(ClientActivityEvent {
                timestamp_unix_ms: crate::now(),
                name: command.name.clone(),
                state: "INTERRUPTED".into(),
            });
            state.last_command = Some(command);
        }
        let activity = Arc::new(Self {
            state: Mutex::new(state),
            store,
        });
        activity.event("Service", "STARTED");
        Ok(activity)
    }
    fn persist(&self, state: &mut ClientActivity) {
        if state.events.len() > 100 {
            state.events.drain(..state.events.len() - 100);
        }
        state.log_available = true;
        if serde_json::to_vec(state)
            .ok()
            .and_then(|bytes| self.store.write("activity.json", &bytes).ok())
            .is_none()
        {
            state.log_available = false;
            tracing::warn!(event = "device_activity_log", result = "unavailable");
        }
    }
    pub fn snapshot(&self) -> ClientActivity {
        self.state.lock().unwrap_or_else(|e| e.into_inner()).clone()
    }
    pub fn event(&self, name: &str, result: &str) {
        let mut state = self.state.lock().unwrap_or_else(|e| e.into_inner());
        state.events.push(ClientActivityEvent {
            timestamp_unix_ms: crate::now(),
            name: safe(name),
            state: safe(result),
        });
        self.persist(&mut state);
    }
    pub fn begin(self: &Arc<Self>, name: &str) -> Command {
        let id = format!("{}-{}", crate::now(), NEXT.fetch_add(1, Ordering::Relaxed));
        let name = safe(name);
        let mut state = self.state.lock().unwrap_or_else(|e| e.into_inner());
        state.active.push(ClientCommandActivity {
            id: id.clone(),
            name: name.clone(),
            started_at_unix_ms: crate::now(),
            finished_at_unix_ms: None,
            state: ClientCommandState::Running,
            progress_percent: None,
        });
        state.events.push(ClientActivityEvent {
            timestamp_unix_ms: crate::now(),
            name,
            state: "RUNNING".into(),
        });
        self.persist(&mut state);
        Command {
            activity: self.clone(),
            id,
            finished: false,
        }
    }
}
pub struct Command {
    activity: Arc<Activity>,
    id: String,
    finished: bool,
}
impl Command {
    pub fn finish(mut self, succeeded: bool) {
        self.complete(if succeeded {
            ClientCommandState::Succeeded
        } else {
            ClientCommandState::Failed
        });
    }
    fn complete(&mut self, result: ClientCommandState) {
        let mut state = self
            .activity
            .state
            .lock()
            .unwrap_or_else(|e| e.into_inner());
        if let Some(index) = state.active.iter().position(|c| c.id == self.id) {
            let mut command = state.active.remove(index);
            command.finished_at_unix_ms = Some(crate::now());
            command.progress_percent = (result == ClientCommandState::Succeeded).then_some(100);
            command.state = result;
            state.events.push(ClientActivityEvent {
                timestamp_unix_ms: crate::now(),
                name: command.name.clone(),
                state: format!("{:?}", command.state).to_uppercase(),
            });
            state.last_command = Some(command);
            self.activity.persist(&mut state);
        }
        self.finished = true;
    }
}
impl Drop for Command {
    fn drop(&mut self) {
        if !self.finished {
            self.complete(ClientCommandState::Interrupted);
        }
    }
}
