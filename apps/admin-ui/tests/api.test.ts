// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
import { describe, it, expect, vi } from 'vitest';
import { ApiError, ControlClient } from '../src/api';
const user = {id:'namespace:user/item',name:'User',enabled:true,revision:7};

describe('Control transport contract', () => {
  it('uses generated routes, same origin, bounded pages and safe token transport', async () => {
    const transport = vi.fn<typeof fetch>().mockResolvedValue(new Response(JSON.stringify({items:[user],nextCursor:'next/+'})));
    const client = new ControlClient('private-token',vi.fn(),transport);
    expect((await client.list('users','next/+')).items).toEqual([user]);
    const [url,options] = transport.mock.calls[0];
    expect(url).toBe('/api/control/v1/users?limit=50&cursor=next%2F%2B');
    expect(new Headers(options?.headers).get('Authorization')).toBe('Bearer private-token');
    expect(options).toMatchObject({credentials:'omit',cache:'no-store',redirect:'error'});
  });
  it('encodes arbitrary canonical identifiers and sends concurrency/idempotency headers', async () => {
    const transport = vi.fn<typeof fetch>().mockImplementation(async () => new Response(JSON.stringify(user)));
    const client = new ControlClient('token',vi.fn(),transport);
    await client.user(user.id); await client.saveUser(user,true,'retry-key'); await client.saveUser({...user,revision:1},false,'create-key');
    expect(transport.mock.calls[0][0]).toBe('/api/control/v1/users/namespace%3Auser%2Fitem');
    const update = transport.mock.calls[1][1]!;
    expect(update.method).toBe('PUT'); expect(new Headers(update.headers).get('If-Match')).toBe('"7"');
    expect(new Headers(update.headers).get('Idempotency-Key')).toBe('retry-key');
    expect(new Headers(transport.mock.calls[2][1]!.headers).has('If-Match')).toBe(false);
    transport.mockResolvedValueOnce(new Response(null,{status:204})); await client.deleteUser(user,'delete-key');
    expect(transport.mock.calls[3][1]?.method).toBe('DELETE');
  });
  it.each([400,403,404,409,413,500,503])('renders only safe messages for HTTP %i', async status => {
    const client = new ControlClient('private-token',vi.fn(),vi.fn().mockResolvedValue(new Response(JSON.stringify({code:'FORBIDDEN',requestId:'safe-id',message:'private-token'}),{status})));
    await expect(client.list('teams')).rejects.toMatchObject({status,requestId:'safe-id'});
    try { await client.list('teams'); } catch(error) { expect(String(error)).not.toContain('private-token'); }
  });
  it('invalidates the whole session on 401 and cannot send again', async () => {
    const denied = vi.fn(); const transport = vi.fn().mockResolvedValue(new Response('{}',{status:401}));
    const client = new ControlClient('token',denied,transport);
    await expect(client.list('users')).rejects.toBeInstanceOf(ApiError); expect(denied).toHaveBeenCalledOnce();
    await expect(client.list('users')).rejects.toMatchObject({name:'AbortError'}); expect(transport).toHaveBeenCalledOnce();
  });
  it('handles empty errors, malformed bodies and unsafe correlation metadata', async () => {
    const transport = vi.fn().mockResolvedValueOnce(new Response(null,{status:413})).mockResolvedValueOnce(new Response('<html>secret</html>')).mockResolvedValueOnce(new Response(JSON.stringify({code:'secret with spaces',requestId:'<secret>'}),{status:503}));
    const client = new ControlClient('token',vi.fn(),transport);
    await expect(client.list('tools')).rejects.toMatchObject({status:413});
    await expect(client.list('tools')).rejects.toMatchObject({code:'INVALID_RESPONSE'});
    await expect(client.list('tools')).rejects.toMatchObject({code:'HTTP_ERROR',requestId:undefined});
  });
  it('bounds response memory and classifies network, timeout and cancellation', async () => {
    const client = new ControlClient('token',vi.fn(),vi.fn().mockResolvedValue(new Response('x'.repeat(2*1024*1024+1))));
    await expect(client.list('policies')).rejects.toMatchObject({code:'RESPONSE_LIMIT'});
    const failed = new ControlClient('token',vi.fn(),vi.fn().mockRejectedValue(new Error('secret')));
    await expect(failed.list('agents')).rejects.toMatchObject({code:'NETWORK'});
    const abort = new AbortController(); abort.abort();
    await expect(failed.list('agents',undefined,abort.signal)).rejects.toMatchObject({name:'AbortError'});
    const timeout = new AbortController(); timeout.abort(); const spy = vi.spyOn(AbortSignal,'timeout').mockReturnValue(timeout.signal);
    await expect(failed.list('agents')).rejects.toMatchObject({code:'TIMEOUT'}); spy.mockRestore();
  });
  it('disposal aborts a pending request', async () => {
    const client = new ControlClient('token',vi.fn(),(_url,options) => new Promise((_resolve,reject) => options!.signal!.addEventListener('abort',() => reject(new Error('aborted')))));
    const pending = client.list('devices'); client.dispose(); await expect(pending).rejects.toMatchObject({name:'AbortError'});
  });
});
