// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
//! Length-framed, bounded OS-authenticated IPC; no remote listener or credential export.
use crate::{contracts::Contracts, service::ClientService, Failure, Result};
use olo_toolgate_contracts::*;
use std::{sync::Arc, time::Duration};
use tokio::io::{AsyncRead, AsyncReadExt, AsyncWrite, AsyncWriteExt};
pub fn authorized(peer: &str, allowed: &[String]) -> bool {
    allowed.iter().any(|entry| entry == peer)
}
async fn read<S: AsyncRead + Unpin>(stream: &mut S) -> Result<Vec<u8>> {
    let size = stream.read_u32().await.map_err(|_| Failure::Unavailable)? as usize;
    if size == 0 || size > 4096 {
        return Err(Failure::Validation);
    }
    let mut bytes = vec![0; size];
    stream
        .read_exact(&mut bytes)
        .await
        .map_err(|_| Failure::Unavailable)?;
    Ok(bytes)
}
async fn write<S: AsyncWrite + Unpin>(stream: &mut S, bytes: &[u8]) -> Result<()> {
    if bytes.len() > 16384 {
        return Err(Failure::Validation);
    }
    stream
        .write_u32(bytes.len() as u32)
        .await
        .map_err(|_| Failure::Unavailable)?;
    stream
        .write_all(bytes)
        .await
        .map_err(|_| Failure::Unavailable)?;
    stream.flush().await.map_err(|_| Failure::Unavailable)
}
fn code(failure: Failure) -> ErrorCode {
    match failure {
        Failure::Unauthorized | Failure::Revoked => ErrorCode::Forbidden,
        Failure::Validation => ErrorCode::Validation,
        Failure::Conflict => ErrorCode::Conflict,
        Failure::Expired => ErrorCode::Timeout,
        Failure::Unsupported => ErrorCode::Unsupported,
        Failure::Unavailable => ErrorCode::DependencyUnavailable,
    }
}
pub async fn connection<S: AsyncRead + AsyncWrite + Unpin>(
    stream: &mut S,
    peer: &str,
    peers: &[String],
    service: &Arc<tokio::sync::Mutex<ClientService>>,
    contracts: &Contracts,
) -> Result<()> {
    if !authorized(peer, peers) {
        return Err(Failure::Unauthorized);
    }
    let bytes = tokio::time::timeout(Duration::from_secs(5), read(stream))
        .await
        .map_err(|_| Failure::Unavailable)??;
    handle(stream, peer, peers, service, contracts, &bytes).await
}
async fn handle<S: AsyncWrite + Unpin>(
    stream: &mut S,
    peer: &str,
    peers: &[String],
    service: &Arc<tokio::sync::Mutex<ClientService>>,
    contracts: &Contracts,
    bytes: &[u8],
) -> Result<()> {
    if !authorized(peer, peers) {
        return Err(Failure::Unauthorized);
    }
    let request: ClientIpcRequest = contracts.decode("ClientIpcRequest", bytes)?;
    let mut response = ClientIpcResponse {
        request_id: request.request_id,
        health: None,
        challenge: None,
        error: None,
    };
    // Busy service returns backpressure instead of accumulating an unbounded local queue.
    let mut state = service.try_lock().map_err(|_| Failure::Conflict)?;
    let result = match request.operation {
        ClientIpcOperation::Health => {
            response.health = Some(state.health());
            Ok(())
        }
        ClientIpcOperation::Enroll => state
            .enroll()
            .await
            .map(|challenge| response.challenge = Some(challenge)),
        ClientIpcOperation::CheckIn => state
            .tick()
            .await
            .map(|_| response.health = Some(state.health())),
    };
    if let Err(failure) = result {
        response.error = Some(code(failure));
    }
    let bytes = contracts.encode("ClientIpcResponse", &response)?;
    drop(state);
    tokio::time::timeout(Duration::from_secs(5), write(stream, &bytes))
        .await
        .map_err(|_| Failure::Unavailable)?
}
#[cfg(unix)]
pub async fn listen(
    endpoint: &str,
    peers: Vec<String>,
    service: Arc<tokio::sync::Mutex<ClientService>>,
    contracts: Arc<Contracts>,
    mut shutdown: tokio::sync::watch::Receiver<bool>,
) -> Result<()> {
    use std::os::unix::fs::PermissionsExt;
    let path = std::path::Path::new(endpoint);
    crate::storage::check_parents(path)?;
    if path.exists() {
        use std::os::unix::fs::{FileTypeExt, MetadataExt};
        let meta = std::fs::symlink_metadata(path).map_err(|_| Failure::Unavailable)?;
        if !meta.file_type().is_socket() || meta.uid() != unsafe { libc::geteuid() } {
            return Err(Failure::Unauthorized);
        }
        // Service lease is already held, so only a stale socket from this identity is removed.
        std::fs::remove_file(path).map_err(|_| Failure::Unavailable)?;
    }
    let listener = tokio::net::UnixListener::bind(path).map_err(|_| Failure::Unavailable)?;
    std::fs::set_permissions(path, std::fs::Permissions::from_mode(0o666))
        .map_err(|_| Failure::Unavailable)?;
    let limit = Arc::new(tokio::sync::Semaphore::new(16));
    let mut tasks = tokio::task::JoinSet::new();
    loop {
        tokio::select! {
            _=shutdown.changed()=>break,
            incoming=listener.accept()=>{
                let(mut stream,_)=incoming.map_err(|_|Failure::Unavailable)?;let permit=match limit.clone().try_acquire_owned(){Ok(p)=>p,Err(_)=>continue};
                let peer=stream.peer_cred().map_err(|_|Failure::Unauthorized)?.uid().to_string();
                let service=service.clone();let contracts=contracts.clone();let peers=peers.clone();
                tasks.spawn(async move{let _permit=permit;let _=tokio::time::timeout(Duration::from_secs(20),connection(&mut stream,&peer,&peers,&service,&contracts)).await;});
            },
            Some(_)=tasks.join_next()=>{},
        }
    }
    tasks.abort_all();
    while tasks.join_next().await.is_some() {}
    drop(listener);
    std::fs::remove_file(path).map_err(|_| Failure::Unavailable)
}
#[cfg(windows)]
pub async fn listen(
    endpoint: &str,
    peers: Vec<String>,
    service: Arc<tokio::sync::Mutex<ClientService>>,
    contracts: Arc<Contracts>,
    mut shutdown: tokio::sync::watch::Receiver<bool>,
) -> Result<()> {
    use std::os::windows::io::AsRawHandle;
    use tokio::net::windows::named_pipe::ServerOptions;
    fn create(
        endpoint: &str,
        peers: &[String],
        first: bool,
    ) -> Result<tokio::net::windows::named_pipe::NamedPipeServer> {
        let descriptor = crate::platform::windows::Descriptor::new(peers)?;
        let mut attributes = descriptor.attributes();
        unsafe {
            ServerOptions::new()
                .first_pipe_instance(first)
                .reject_remote_clients(true)
                .max_instances(17)
                .create_with_security_attributes_raw(
                    endpoint,
                    (&mut attributes as *mut windows_sys::Win32::Security::SECURITY_ATTRIBUTES)
                        .cast(),
                )
        }
        .map_err(|_| Failure::Unauthorized)
    }
    let mut pipe = create(endpoint, &peers, true)?;
    let mut tasks = tokio::task::JoinSet::new();
    let limit = Arc::new(tokio::sync::Semaphore::new(16));
    loop {
        tokio::select! {
            _=shutdown.changed()=>break,
            connected=pipe.connect()=>{
                connected.map_err(|_|Failure::Unavailable)?;
                let next=create(endpoint,&peers,false)?;
                let mut stream=std::mem::replace(&mut pipe,next);let permit=match limit.clone().try_acquire_owned(){Ok(p)=>p,Err(_)=>continue};
                let service=service.clone();let contracts=contracts.clone();let peers=peers.clone();
                tasks.spawn(async move{let _permit=permit;let _=tokio::time::timeout(Duration::from_secs(20),async{
                    // Impersonation authenticates the token associated with data actually read.
                    let bytes=read(&mut stream).await?;let peer=crate::platform::windows::peer_sid(stream.as_raw_handle())?;
                    handle(&mut stream,&peer,&peers,&service,&contracts,&bytes).await
                }).await;});
            },
            Some(_)=tasks.join_next()=>{},
        }
    }
    tasks.abort_all();
    while tasks.join_next().await.is_some() {}
    Ok(())
}
pub async fn call(endpoint: &str, operation: ClientIpcOperation) -> Result<ClientIpcResponse> {
    let contracts = Contracts::new()?;
    let request = ClientIpcRequest {
        protocol_version: 1,
        request_id: crate::identity::nonce()?,
        operation,
    };
    let bytes = contracts.encode("ClientIpcRequest", &request)?;
    #[cfg(unix)]
    let mut stream = tokio::net::UnixStream::connect(endpoint)
        .await
        .map_err(|_| Failure::Unavailable)?;
    #[cfg(unix)]
    if stream.peer_cred().map_err(|_| Failure::Unauthorized)?.uid() != 0 {
        return Err(Failure::Unauthorized);
    }
    #[cfg(windows)]
    let mut stream = tokio::net::windows::named_pipe::ClientOptions::new()
        .open(endpoint)
        .map_err(|_| Failure::Unavailable)?;
    #[cfg(windows)]
    {
        use std::os::windows::io::AsRawHandle;
        if crate::platform::windows::server_sid(stream.as_raw_handle())? != "S-1-5-18" {
            return Err(Failure::Unauthorized);
        }
    }
    write(&mut stream, &bytes).await?;
    let size = stream.read_u32().await.map_err(|_| Failure::Unavailable)? as usize;
    if size == 0 || size > 16384 {
        return Err(Failure::Validation);
    }
    let mut bytes = vec![0; size];
    stream
        .read_exact(&mut bytes)
        .await
        .map_err(|_| Failure::Unavailable)?;
    let response: ClientIpcResponse = contracts.decode("ClientIpcResponse", &bytes)?;
    if response.request_id != request.request_id {
        return Err(Failure::Unauthorized);
    }
    Ok(response)
}
