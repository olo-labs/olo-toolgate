// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
import type { ApprovalDecisionRequest, ApprovalPage, ApprovalRecord, ControlUser, ErrorEnvelope, EndpointEnrollmentReview, EndpointEnrollmentDecision } from '@olo-labs/toolgate-contracts';
import { listOperations, operations, type DirectoryKind, type DirectoryPages } from './operations.generated';

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
  private token: string;
  private readonly lifetime = new AbortController();
  constructor(token: string, private readonly onUnauthorized: () => void, private readonly transport: typeof fetch = (input, init) => fetch(input, init)) {
    this.token = token;
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

  list<K extends DirectoryKind>(kind: K, cursor?: string, signal?: AbortSignal): Promise<DirectoryPages[K]> {
    const query = new URLSearchParams({ limit: '50' }); if (cursor) query.set('cursor', cursor);
    return this.send(listOperations[kind], { query, signal });
  }
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
  decideEnrollment(decision: EndpointEnrollmentDecision, key: string): Promise<EndpointEnrollmentReview> {
    return this.send(operations.decideEndpointEnrollment, { body: decision, key });
  }
}
