// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
import type { FleetReleasePage, FleetRolloutPage, FleetRolloutRequest, FleetRolloutRecord, FleetPackageRelease, ApprovalDecisionRequest, ApprovalPage, ApprovalRecord, ControlUser, ErrorEnvelope, EndpointEnrollmentReview, EndpointEnrollmentDecision } from '@olo-labs/toolgate-contracts';
import { listOperations, getOperations, createOperations, updateOperations, deleteOperations, operations, type DirectoryKind, type DirectoryPages, type DirectoryRecords } from './operations.generated';
import type { BuilderDraft, BuilderDraftPage, BuilderDraftRequest, BuilderTestPage, BuilderTestRequest, BuilderTestRecord, BuilderDefinition } from '@olo-labs/toolgate-contracts';
import type { AdminSession, ControlAuditPage } from '@olo-labs/toolgate-contracts';
import type {RemoteToolPage} from '@olo-labs/toolgate-contracts';

/** Human-safe messages never render server text, exception bodies or credentials. */
export class ApiError extends Error {
  constructor(readonly status: number, readonly code: string, readonly requestId?: string) {
    super(status === 401 ? 'Your session ended. Connect again with a valid access token.'
      : status === 403 ? 'Your account does not have permission for this action.'
      : status === 409 ? 'This record changed or conflicts with another record. Refresh before trying again.'
      : status === 400 ? 'The server rejected these values. Check the fields and references.'
      : status === 404 ? 'This record is no longer available.'
      : status === 413 ? 'This request is too large.'
      : code === 'TIMEOUT' ? 'The request timed out. You can try again.'
      : status === 0 ? 'Unable to reach Control. Check your connection and try again.'
      : 'Control could not complete this request. Try again shortly.');
  }
}

type Operation = { readonly method: string; readonly path: string };
const MAX_RESPONSE = 2 * 1024 * 1024;

async function readBody(response: Response): Promise<unknown> {
  const reader = response.body?.getReader();
  if (!reader) return undefined;
  const chunks: Uint8Array[] = []; let size = 0;
  try {
    while (true) {
      const { value, done } = await reader.read();
      if (done) break;
      size += value.byteLength;
      if (size > MAX_RESPONSE) throw new ApiError(502, 'RESPONSE_LIMIT');
      chunks.push(value);
    }
  } finally { await reader.cancel(); }
  const bytes = new Uint8Array(size); let offset = 0;
  for (const chunk of chunks) { bytes.set(chunk, offset); offset += chunk.byteLength; }
  if (!size) return undefined;
  try { return JSON.parse(new TextDecoder().decode(bytes)); }
  catch { throw new ApiError(502, 'INVALID_RESPONSE'); }
}

/** Same-origin transport. Backend exclusively owns auth, validation and concurrency.
 * A session disposal clears credentials and aborts all outstanding requests.
 * Mutations are never automatically retried; callers retain a key for an exact retry.
 */
export class ControlClient {
  remoteRequests(signal?:AbortSignal):Promise<RemoteToolPage>{return this.send(operations.listLocalMcpRequests,{signal});}
  private token: string;
  private readonly lifetime = new AbortController();
  constructor(token: string, private readonly onUnauthorized: () => void, private readonly transport: typeof fetch = (input, init) => fetch(input, init)) {
    this.token = token;
  }
  quickstart<T>(path: 'tools' | 'invoke' | 'vault', body?: unknown): Promise<T> {
    return this.send<T>({method: body === undefined ? 'GET' : 'POST', path: `/api/quickstart/v1/${path}`}, body === undefined ? {} : {body});
  }
  dispose(): void { this.token = ''; this.lifetime.abort(); }

  private async send<T>(operation: Operation, options: { id?: string; query?: URLSearchParams; body?: unknown; revision?: number; key?: string; signal?: AbortSignal } = {}): Promise<T> {
    if (this.lifetime.signal.aborted) throw new DOMException('Session ended', 'AbortError');
    const timeout = AbortSignal.timeout(15000);
    const signal = AbortSignal.any([this.lifetime.signal, timeout, ...(options.signal ? [options.signal] : [])]);
    const path = operation.path.replace('{id}', encodeURIComponent(options.id ?? ''));
    const headers = new Headers({ Accept: 'application/json', Authorization: `Bearer ${this.token}` });
    if (options.body !== undefined) headers.set('Content-Type', 'application/json');
    if (options.key) headers.set('Idempotency-Key', options.key);
    if (options.revision !== undefined) headers.set('If-Match', `"${options.revision}"`);
    let response: Response;
    try {
      response = await this.transport(path + (options.query ? `?${options.query}` : ''), {
        method: operation.method, headers, body: options.body === undefined ? undefined : JSON.stringify(options.body),
        signal, credentials: 'omit', cache: 'no-store', redirect: 'error',
      });
      if (response.status === 401) { this.dispose(); this.onUnauthorized(); }
      const body = await readBody(response);
      if (!response.ok) {
        const envelope = body as Partial<ErrorEnvelope> | undefined;
        const code = typeof envelope?.code === 'string' && /^[A-Z_]{1,64}$/.test(envelope.code) ? envelope.code : 'HTTP_ERROR';
        const requestId = response.headers.get('X-Request-ID') ?? envelope?.requestId;
        throw new ApiError(response.status, code, typeof requestId === 'string' && /^[a-zA-Z0-9._:/-]{1,128}$/.test(requestId) ? requestId : undefined);
      }
      return body as T;
    } catch (error) {
      if (error instanceof ApiError) throw error;
      if (this.lifetime.signal.aborted || options.signal?.aborted) throw new DOMException('Request cancelled', 'AbortError');
      throw new ApiError(0, timeout.aborted ? 'TIMEOUT' : 'NETWORK');
    }
  }

  fleetReleases(cursor?:string,signal?:AbortSignal):Promise<FleetReleasePage>{const query=new URLSearchParams();if(cursor)query.set('cursor',cursor);return this.send(operations.listFleetReleases,{query,signal});}
  builderDrafts(cursor?:string,signal?:AbortSignal):Promise<BuilderDraftPage>{const query=new URLSearchParams();if(cursor)query.set('cursor',cursor);return this.send(operations.listBuilderDrafts,{query,signal});}
  builderTests(cursor?:string,signal?:AbortSignal):Promise<BuilderTestPage>{const query=new URLSearchParams();if(cursor)query.set('cursor',cursor);return this.send(operations.listBuilderTests,{query,signal});}
  saveDraft(body:BuilderDraftRequest,key:string):Promise<BuilderDraft>{return this.send(operations.saveBuilderDraft,{body,key});}
  testDraft(body:BuilderTestRequest,key:string):Promise<BuilderTestRecord>{return this.send(operations.createBuilderTest,{body,key});}
  sealDraft(id:string,expectedRevision:number,key:string):Promise<BuilderDraft>{return this.send(operations.sealBuilderDraft,{id,key,body:{expectedRevision}});}
  publicationDraft(id:string):Promise<BuilderDefinition>{return this.send(operations.prepareBuilderPublication,{id});}
  releaseDraft(id:string,body:FleetPackageRelease,key:string):Promise<FleetPackageRelease>{return this.send(operations.publishBuilderRelease,{id,body,key});}
  deployDraft(id:string,body:FleetRolloutRequest,key:string):Promise<FleetRolloutRecord>{return this.send(operations.deployBuilderDraft,{id,body,key});}
  fleetRollouts(cursor?:string,signal?:AbortSignal):Promise<FleetRolloutPage>{const query=new URLSearchParams();if(cursor)query.set('cursor',cursor);return this.send(operations.listFleetRollouts,{query,signal});}
  publishRelease(release:FleetPackageRelease,key:string):Promise<FleetPackageRelease>{return this.send(operations.publishFleetRelease,{body:release,key});}
  assignPackage(request:FleetRolloutRequest,key:string):Promise<FleetRolloutRecord>{return this.send(operations.createFleetRollout,{body:request,key});}
  advanceRollout(id:string,expectedRevision:number,percentage:number,key:string):Promise<FleetRolloutRecord>{return this.send(operations.advanceFleetRollout,{id,key,body:{expectedRevision,percentage}});}
  list<K extends DirectoryKind>(kind: K, cursor?: string, signal?: AbortSignal): Promise<DirectoryPages[K]> {
    const query = new URLSearchParams({ limit: '50' }); if (cursor) query.set('cursor', cursor);
    return this.send(listOperations[kind], { query, signal });
  }
  adminSession():Promise<AdminSession>{return this.send(operations.getAdminSession);}
  audit(cursor:string,signal?:AbortSignal):Promise<ControlAuditPage>{return this.send(operations.listAudit,{query:new URLSearchParams({cursor,limit:'50'}),signal});}
  record<K extends DirectoryKind>(kind:K,id:string):Promise<DirectoryRecords[K]> {return this.send(getOperations[kind],{id});}
  saveRecord<K extends DirectoryKind>(kind:K,record:DirectoryRecords[K],existing:boolean,key:string):Promise<DirectoryRecords[K]> {
    return this.send(existing?updateOperations[kind]:createOperations[kind],{id:record.id,body:record,key,revision:existing?record.revision:undefined});
  }
  deleteRecord<K extends DirectoryKind>(kind:K,record:DirectoryRecords[K],key:string):Promise<void> {return this.send(deleteOperations[kind],{id:record.id,revision:record.revision,key});}
  user(id: string, signal?: AbortSignal): Promise<ControlUser> { return this.send(operations.getControlUser, { id, signal }); }
  saveUser(user: ControlUser, existing: boolean, key: string): Promise<ControlUser> {
    return this.send(existing ? operations.updateControlUser : operations.createControlUser,
      { id: user.id, body: user, key, revision: existing ? user.revision : undefined });
  }
  deleteUser(user: ControlUser, key: string): Promise<void> {
    return this.send(operations.deleteControlUser, { id: user.id, revision: user.revision, key });
  }
  approvals(cursor?: string, signal?: AbortSignal): Promise<ApprovalPage> {
    const query = new URLSearchParams({ limit: '50' }); if (cursor) query.set('cursor', cursor);
    return this.send(operations.listApprovals, { query, signal });
  }
  approval(id: string, signal?: AbortSignal): Promise<ApprovalRecord> { return this.send(operations.getApproval, { id, signal }); }
  decideApproval(id: string, decision: ApprovalDecisionRequest, key: string): Promise<ApprovalRecord> {
    return this.send(operations.decideApproval, { id, body: decision, key });
  }
  enrollment(code: string, signal?: AbortSignal): Promise<EndpointEnrollmentReview> {
    return this.send(operations.reviewEndpointEnrollment, { query: new URLSearchParams({ code }), signal });
  }
  endpointDevice(id:string,signal?:AbortSignal):Promise<import('@olo-labs/toolgate-contracts').EndpointDeviceRecord>{return this.send(operations.getEndpointDevice,{id,signal});}
  decideEnrollment(decision: EndpointEnrollmentDecision, key: string): Promise<EndpointEnrollmentReview> {
    return this.send(operations.decideEndpointEnrollment, { body: decision, key });
  }
}
