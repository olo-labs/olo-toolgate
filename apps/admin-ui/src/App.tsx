// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
import { useEffect, useRef, useState, type FormEvent } from 'react';
import type { ControlUser } from '@olo-labs/toolgate-contracts';
import { ApiError, ControlClient } from './api';
import type { DirectoryKind, DirectoryRecords, DirectoryPages } from './operations.generated';
import { Failure } from './Failure';
import { Approvals } from './Approvals';
import { Fleet } from './Fleet';
import { Builder } from './Builder';
import { Enrollment } from './Enrollment';
import { QuickstartTools } from './QuickstartTools';
import { ClientDownloads } from './ClientDownloads';
import { ThemePicker } from './Theme';
import { Audit } from './Audit';
import { DirectoryEditor } from './DirectoryEditor';
import oloLogo from './assets/olo.png';

declare const __APP_VERSION__: string;
const directorySections = [ ['users', 'Users'], ['teams', 'Teams'], ['roles', 'Roles'], ['tools', 'Tools'], ['policies', 'Policies'], ['devices', 'Clients'], ['agents', 'Agents'] ] as const;
const sections = [ ['overview', 'Overview'], ...directorySections, ['audit', 'Audit log'], ['approvals', 'Approvals'], ['fleet', 'Packages'], ['builder', 'Tool builder'], ['enroll', 'Enroll device'], ['local', 'Built-in tools and vault'] ] as const;
const navigationGroups=[{label:'Users',routes:['users','teams','roles','agents']},{label:'Tools',routes:['tools','policies','builder','local']},{label:'Devices',routes:['devices','enroll','fleet']},{label:'Audit',routes:['audit','approvals']}] as const;
type Route = typeof sections[number][0];
type RecordValue = DirectoryRecords[DirectoryKind];
const routeFromHash = (): Route => sections.find(([route]) => window.location.hash.split('?')[0] === `#${route}`)?.[0] ?? 'overview';
const safeOrigin = () => window.location.protocol === 'https:' || ['localhost', '127.0.0.1', '[::1]'].includes(window.location.hostname);

/** Authenticated shell; identities/roles are verified only by Control. */
export function App() {
  const quickstart = document.querySelector('meta[name=toolgate-mode]')?.getAttribute('content') === 'quickstart';
  const [newPassword, setNewPassword] = useState('');
  const [passwordDisabled, setPasswordDisabled] = useState(false);
  const [client, setClient] = useState<ControlClient>();
  const [administrator,setAdministrator]=useState(false);
  const [credential, setCredential] = useState(''); const [connecting, setConnecting] = useState(false);
  const [error, setError] = useState<unknown>(); const [route, setRoute] = useState<Route>(routeFromHash);
  const active = useRef<ControlClient | undefined>(undefined);
  useEffect(() => { const update = () => setRoute(routeFromHash()); window.addEventListener('hashchange', update); return () => { window.removeEventListener('hashchange', update); active.current?.dispose(); }; }, []);
  function disconnect() { active.current?.dispose(); active.current = undefined; setClient(undefined); setCredential(''); setConnecting(false); }
  useEffect(() => {
    if (!quickstart || !safeOrigin()) return;
    const abort = new AbortController(); let timer: ReturnType<typeof setInterval> | undefined;
    fetch('/api/quickstart/v1/status', {credentials:'omit',redirect:'error',signal:abort.signal})
      .then(response => response.ok ? response.json() : undefined)
      .then(body => {
        if (!abort.signal.aborted && body?.passwordRequired === false) {
          setPasswordDisabled(true); void connect(null, true);
          timer = setInterval(() => { void connect(null, true); }, 600000);
        }
      }).catch(() => { /* Normal authenticated login remains available. */ });
    return () => { abort.abort(); if (timer) clearInterval(timer); };
  }, [quickstart]);
  async function connect(event: FormEvent | null, automatic = false) {
    event?.preventDefault(); if (connecting || !safeOrigin()) return;
    setConnecting(true); setError(undefined);
    let accessToken = credential.trim();
    if (quickstart) {
      try {
        const response = await fetch('/api/quickstart/v1/login', { method: 'POST', headers: {'Content-Type':'application/json'}, body: JSON.stringify(automatic ? {} : {password:credential, ...(newPassword ? {newPassword} : {})}), credentials:'omit', redirect:'error' });
        if (!response.ok) throw new ApiError(response.status, 'LOGIN_FAILED');
        const body: unknown = await response.json();
        if (!body || typeof body !== 'object' || !('accessToken' in body) || typeof body.accessToken !== 'string' || body.accessToken.length > 16384) throw new ApiError(502, 'INVALID_RESPONSE');
        accessToken = body.accessToken; setNewPassword('');
      } catch (failure) { setCredential(''); setNewPassword(''); setError(failure); setConnecting(false); return; }
    }
    const candidate = new ControlClient(accessToken, () => { if (active.current === candidate) { disconnect(); setError(new ApiError(401, 'UNAUTHORIZED')); } });
    active.current = candidate; setCredential('');
    try {
      try { await candidate.adminSession(); setAdministrator(true); }
      catch (failure) {
        if (!(failure instanceof ApiError) || failure.status !== 403) throw failure;
        setAdministrator(false);
        await candidate.approvals();
        if (active.current === candidate) { window.location.hash = '#approvals'; setRoute('approvals'); }
      }
      if (active.current === candidate) setClient(candidate);
    }
    catch (failure) { if (active.current === candidate) { candidate.dispose(); active.current = undefined; setError(failure); } }
    finally { if (!active.current || active.current === candidate) setConnecting(false); }
  }
  return <><a className="skip" href="#main" onClick={event => { event.preventDefault(); document.getElementById('main')?.focus(); }}>Skip to content</a>
    {!client ? <main id="main" tabIndex={-1} className="connect-page"><header className="login-header"><div className="brand"><img className="brand-logo" src={oloLogo} alt="OLO" /> <span>ToolGate</span></div>
      <ThemePicker /></header><div className="connect-card"><p className="eyebrow">Organization console</p><h1>Manage your workspace</h1>
        {quickstart ? <p role="status" className="notice">Quickstart · Single node · Non-HA. First login requires a new strong password.</p> : <p>Connect to Control with an access token issued by your organization’s identity provider.</p>}
        {!safeOrigin() && <div role="alert" className="notice error">Open this console over HTTPS before connecting.</div>}
        {Boolean(error) && <Failure error={error} />}
        {passwordDisabled ? <><p role="status">Local password-free Quickstart. Connecting automatically.</p><button onClick={() => { void connect(null, true); }} disabled={connecting}>Reconnect</button></> : <form onSubmit={connect}><label htmlFor="access-token">{quickstart ? 'Password' : 'Access token'}</label>
          <input id="access-token" type="password" autoComplete="off" spellCheck={false} maxLength={16384} required value={credential} onChange={e => setCredential(e.target.value)} disabled={connecting || !safeOrigin()} aria-describedby="token-help" />
          {quickstart && <><label htmlFor="new-password">New password (required on first login)</label><input id="new-password" type="password" autoComplete="new-password" minLength={16} maxLength={128} value={newPassword} onChange={e => setNewPassword(e.target.value)} disabled={connecting} /></>}
          <p id="token-help" className="hint">Kept in memory for this session. Refreshing or disconnecting clears it.</p>
          <button className="primary" disabled={connecting || !safeOrigin()}>{connecting ? 'Connecting…' : 'Connect to workspace'}</button>
          {connecting && <p role="status">Verifying your session with Control…</p>}
        </form>}</div><ClientDownloads /><footer>ToolGate {__APP_VERSION__} · Organization administration</footer></main>
    : <div className="shell"><aside className="sidebar"><div className="brand"><img className="brand-logo" src={oloLogo} alt="OLO" /> <span>ToolGate</span></div>
      <p className="sidebar-caption">{administrator?'Workspace':'Approval workspace'}</p><nav aria-label="Main navigation">{administrator?<><a href="#overview" aria-current={route==='overview'?'page':undefined}>Overview</a>{navigationGroups.map(group=><details className="nav-group" key={group.label} open><summary>{group.label}</summary><div className="nav-submenu">{group.routes.filter(value=>quickstart||value!=='local').map(value=><a key={value} href={`#${value}`} aria-current={route===value?'page':undefined}><span className="nav-dot"/>{sections.find(([key])=>key===value)![1]}</a>)}</div></details>)}</>:<a href="#approvals" aria-current="page">Approvals</a>}</nav>
      <div className="sidebar-bottom"><span className="connection">Connected to Control</span><small>v{__APP_VERSION__}</small><button onClick={disconnect}>Disconnect</button></div></aside>
      <div className="workspace"><header className="topbar"><span>{administrator?'Admin Portal':'Approval workspace'}</span><div className="topbar-actions"><span className="tag">{quickstart ? 'Quickstart · Non-HA' : 'Organization workspace'}</span><ThemePicker /></div></header>
        <main id="main" tabIndex={-1}>{!administrator?<Approvals client={client}/>:route === 'local' ? quickstart ? <QuickstartTools client={client} /> : <p>Local tools are available in Quickstart.</p> : route === 'overview' ? <Dashboard client={client} /> : route === 'audit' ? <Audit client={client}/> : route === 'approvals' ? <Approvals client={client} /> : route === 'fleet' ? <Fleet client={client} /> : route === 'builder' ? <Builder client={client} /> : route === 'enroll' ? <><Enrollment client={client} /><ClientDownloads /></> : <Directory key={route} client={client} kind={route} />}</main>
        <footer>Control verifies permissions. Gateway checks current policy for every runtime authorization.</footer></div></div>}
  </>;
}

function Dashboard({ client }: { client: ControlClient }) {
  const [counts,setCounts] = useState<Partial<Record<DirectoryKind,{ count: number; more: boolean }>>>({});
  const [error,setError] = useState<unknown>(); const [attempt,setAttempt] = useState(0);
  useEffect(() => {
    const abort = new AbortController(); setCounts({}); setError(undefined);
    Promise.all(directorySections.map(async ([kind]) => { const page = await client.list(kind, undefined, abort.signal); return [kind,{ count:page.items.length, more:!!page.nextCursor }] as const; }))
      .then(values => { if (!abort.signal.aborted) setCounts(Object.fromEntries(values)); })
      .catch(failure => { if (!abort.signal.aborted) setError(failure); });
    return () => abort.abort();
  },[client,attempt]);
  return <><p className="eyebrow">Workspace overview</p><h1>Your organization, at a glance</h1><p className="intro">A clear place to manage the people and capabilities in your directory.</p>
    {error ? <Failure error={error} retry={() => setAttempt(attempt+1)} /> : Object.keys(counts).length === 0 ? <p role="status">Loading your directory…</p> :
      <div className="stats">{directorySections.map(([kind,label]) => <a className="stat" key={kind} href={`#${kind}`}><span>{label}</span><strong>{counts[kind]?.count}{counts[kind]?.more ? '+' : ''}</strong><small>View directory →</small></a>)}</div>}
    <section className="guidance"><span className="tag">Getting organized</span><h2>Start with the people who use your tools.</h2><p>Add users, then browse teams and registered capabilities. Policies describe stored configuration; runtime distribution is a separate step.</p><a href="#users" className="text-link">Open users →</a></section>
    <p className="hint">Counts show the first page (up to 50 records). A + means more pages are available. Clients show device records, without enrollment or online status.</p></>;
}

function Directory({ client, kind }: { client: ControlClient; kind: DirectoryKind }) {
  const label = sections.find(([value]) => value === kind)![1];
  const [page,setPage] = useState<DirectoryPages[DirectoryKind]>();
  const [cursor,setCursor] = useState<string>(); const [history,setHistory] = useState<(string|undefined)[]>([]);
  const [error,setError] = useState<unknown>(); const [attempt,setAttempt] = useState(0);
  const [selected,setSelected] = useState<RecordValue>(); const [creating,setCreating] = useState(false);
  useEffect(() => {
    const abort = new AbortController(); setPage(undefined); setError(undefined); setSelected(undefined); setCreating(false);
    client.list(kind,cursor,abort.signal).then(result => { if (!abort.signal.aborted) setPage(result); }).catch(failure => { if (!abort.signal.aborted) setError(failure); });
    return () => abort.abort();
  },[client,kind,cursor,attempt]);
  const refresh = () => setAttempt(attempt+1);
  return <><div className="page-heading"><div><p className="eyebrow">Organization directory</p><h1>{label}</h1></div><div className="actions"><button onClick={refresh}>Refresh</button>{<button className="primary" onClick={() => { setSelected(undefined); setCreating(true); }}>Add {kind==='users'?'user':kind==='teams'?'team':kind==='roles'?'role':kind==='tools'?'tool':kind==='policies'?'policy':kind==='agents'?'agent':'client'}</button>}</div></div>
    <p className="intro">{kind === 'users' ? 'Assign roles directly or through team membership. Identity provider authentication remains required for portal access.' : kind === 'policies' ? 'Review who can use what, and where. These records have not been distributed to runtime gateways.' : kind === 'devices' ? 'Manage registered clients. Directory activation and device enrollment are separate approvals.' : `Browse registered ${label.toLowerCase()} in your organization.`}</p>
    {error ? <Failure error={error} retry={refresh} /> : !page ? <p role="status">Loading {label.toLowerCase()}…</p> : <>
      {page.items.length === 0 ? <section className="empty"><h2>No {label.toLowerCase()} on this page</h2><p>{kind === 'users' ? 'Add a directory user to get started.' : 'Records will appear here when they are added to Control.'}</p></section> :
        <div className="table-wrap"><table><caption className="sr-only">{label} directory</caption><thead><tr><th scope="col">Name</th><th scope="col">Identifier</th><th scope="col">Directory status</th>{kind==='users'&&<th scope="col">Role</th>}<th scope="col">Revision</th></tr></thead><tbody>{page.items.map(record => <tr key={record.id}><th scope="row"><button className="record-link" onClick={() => { setSelected(record); setCreating(false); }}>{record.name}</button></th><td><code>{record.id}</code></td><td><span className={`status ${record.enabled ? 'enabled' : ''}`}>{record.enabled ? 'Enabled' : 'Disabled'}</span></td>{kind==='users'&&<td>{'access' in record&&record.access?record.access.roleIds?.length?record.access.roleIds.join(', '):record.access.role==='SUPER_ADMIN'?'Super Admin':record.access.role==='ADMINISTRATOR'?'Administrator':'Basic':'Identity provider'}</td>}<td>{record.revision}</td></tr>)}</tbody></table></div>}
      <div className="pagination"><span>{page.items.length} records on this page</span><div className="actions"><button disabled={!history.length} onClick={() => { setCursor(history.at(-1)); setHistory(history.slice(0,-1)); }}>Previous page</button><button disabled={!page.nextCursor} onClick={() => { setHistory([...history,cursor]); setCursor(page.nextCursor); }}>Next page</button></div></div>
    </>}
    {(creating || selected) && (kind === 'users' ? <UserEditor key={selected?.id ?? 'new'} client={client} user={selected as ControlUser | undefined} close={() => { setCreating(false); setSelected(undefined); }} saved={refresh} /> : <DirectoryEditor key={selected?.id??'new'} client={client} kind={kind} record={selected as DirectoryRecords[Exclude<DirectoryKind,'users'>] | undefined} close={()=>{setCreating(false);setSelected(undefined);}} saved={refresh}/>)}
  </>;
}

function UserEditor({ client, user, close, saved }: { client: ControlClient; user?: ControlUser; close: () => void; saved: () => void }) {
  const [id,setId] = useState(user?.id ?? ''); const [name,setName] = useState(user?.name ?? ''); const [enabled,setEnabled] = useState(user?.enabled ?? false);
  const [current,setCurrent] = useState(user); const [busy,setBusy] = useState(false); const [error,setError] = useState<unknown>(); const [confirm,setConfirm] = useState(false);
  const [roleIds,setRoleIds]=useState(user?.access?.roleIds?.join(', ')??'');
  const heading = useRef<HTMLHeadingElement>(null); const alive = useRef(true);
  const pending = useRef<{ body: string; key: string } | undefined>(undefined);
  useEffect(() => { alive.current = true; heading.current?.focus(); return () => { alive.current = false; }; },[]);
  const mutationKey = (body: string) => { if (pending.current?.body !== body) pending.current = {body,key:crypto.randomUUID()}; return pending.current!.key; };
  async function submit(event: FormEvent) {
    event.preventDefault(); if (busy) return; setBusy(true); setError(undefined);
    const record: ControlUser = {id,name,enabled,revision:current?.revision ?? 1,access:{role:current?.access?.role??'BASIC',templateIds:current?.access?.templateIds??[],deviceGroupIds:current?.access?.deviceGroupIds??[],roleIds:roleIds.split(',').map(value=>value.trim()).filter(Boolean)}};
    try { await client.saveUser(record,!!current,mutationKey(JSON.stringify(record))); if (alive.current) saved(); }
    catch (failure) { if (alive.current) setError(failure); } finally { if (alive.current) setBusy(false); }
  }
  async function reload() {
    if (!current || busy) return; setBusy(true); setError(undefined);
    try { const record = await client.user(current.id); if (alive.current) { setCurrent(record); setName(record.name); setEnabled(record.enabled);setRoleIds(record.access?.roleIds?.join(', ')??''); pending.current = undefined; } }
    catch (failure) { if (alive.current) setError(failure); } finally { if (alive.current) setBusy(false); }
  }
  async function remove() {
    if (!current || busy) return; setBusy(true); setError(undefined);
    try { await client.deleteUser(current,mutationKey(`delete:${current.id}:${current.revision}`)); if (alive.current) saved(); }
    catch (failure) { if (alive.current) setError(failure); } finally { if (alive.current) setBusy(false); }
  }
  return <section className="detail" aria-label="User editor"><div className="page-heading"><h2 ref={heading} tabIndex={-1}>{current ? 'Edit user' : 'Add directory user'}</h2><button onClick={close} disabled={busy}>Close editor</button></div>
    {Boolean(error) && <Failure error={error} />}
    <form onSubmit={submit} className="user-form"><label htmlFor="user-id">Identifier</label><input id="user-id" value={id} required maxLength={128} disabled={!!current || busy} onChange={e => setId(e.target.value)} />
      <label htmlFor="user-name">Display name</label><input id="user-name" value={name} required maxLength={200} disabled={busy} onChange={e => setName(e.target.value)} />
      <label className="checkbox"><input type="checkbox" checked={enabled} disabled={busy} onChange={e => setEnabled(e.target.checked)} />Enabled in directory</label>
      <label htmlFor="user-roles">Role IDs, separated by commas</label><input id="user-roles" value={roleIds} disabled={busy} onChange={e=>setRoleIds(e.target.value)}/>
      <p className="hint">Assign enabled roles from Users → Roles. Team roles also apply to members. Privilege templates and permission JSON are managed on the role, rather than on individual users. Only Super Admin can change role assignments.</p>
      {current?.access && <p className="hint">Existing identity classification: {current.access.role}. Existing direct privileges are retained for compatibility; assigning managed roles replaces their runtime template grants.</p>}
      <p className="hint">New users start disabled and join the configured default team and policy scope. Manage membership in Teams; a user can belong to multiple teams. The server validates all values and references. {current && `Editing revision ${current.revision}.`}</p><div className="actions"><button className="primary" disabled={busy}>{busy ? 'Working…' : 'Save user'}</button>{current && <button type="button" disabled={busy} onClick={reload}>Reload current record</button>}</div>
    </form>
    {current && <div className="delete-area">{confirm ? <><p>Delete this directory record? Its identifier cannot be reused. Referenced users cannot be deleted.</p><button disabled={busy} className="danger" onClick={remove}>Confirm delete</button> <button disabled={busy} onClick={() => setConfirm(false)}>Cancel delete</button></> : <button className="danger" disabled={busy} onClick={() => setConfirm(true)}>Delete user</button>}</div>}
  </section>;
}
