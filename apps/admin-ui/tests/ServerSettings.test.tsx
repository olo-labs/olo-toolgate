// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
import {describe,it,expect,vi,afterEach} from 'vitest';
import {render,screen,fireEvent,cleanup} from '@testing-library/react';
import {ServerSettings,importedSettings} from '../src/ServerSettings';
import {ControlClient} from '../src/api';
afterEach(cleanup);
const initial={formatVersion:1,revision:0,autoApproveDevices:false,autoApproveDurationDays:30,gatewayName:'ToolGate'};
describe('Server settings',()=>{
  it('rejects files that are not exported settings',()=>{
    expect(importedSettings({...initial,autoApproveOwnerUserId:'admin'})).toEqual({gatewayName:'ToolGate',autoApproveDevices:false,autoApproveDurationDays:'30',autoApproveOwnerUserId:'admin'});
    expect(()=>importedSettings({...initial,gatewayName:' spaced'})).toThrow();
    expect(()=>importedSettings({...initial,formatVersion:2})).toThrow();
    expect(()=>importedSettings({...initial,autoApproveDurationDays:0})).toThrow();
  });
  it('defaults to 30 days and saves auto-approval with an owner and the current revision',async()=>{
    const transport=vi.fn<typeof fetch>().mockImplementation(async(_url,options)=>new Response(options?.method==='PUT'?JSON.stringify({...JSON.parse(String(options.body)),revision:1}):JSON.stringify(initial),{status:200}));
    render(<ServerSettings client={new ControlClient('token',vi.fn(),transport)}/>);
    const duration=await screen.findByLabelText('Auto approve duration (days)');expect((duration as HTMLInputElement).value).toBe('30');
    fireEvent.click(screen.getByLabelText('Auto approve devices'));
    const save=screen.getByRole('button',{name:'Save server settings'});expect((save as HTMLButtonElement).disabled).toBe(true);
    fireEvent.change(screen.getByLabelText('Owner user ID for auto-approved devices'),{target:{value:'admin'}});
    fireEvent.change(screen.getByLabelText('Gateway name'),{target:{value:'Lab Gateway'}});
    fireEvent.click(save);await screen.findByText('Server settings saved.');
    const call=transport.mock.calls[1];expect(call[0]).toBe('/api/control/v1/settings');
    expect(JSON.parse(String(call[1]?.body))).toEqual({formatVersion:1,revision:0,gatewayName:'Lab Gateway',autoApproveDevices:true,autoApproveDurationDays:30,autoApproveOwnerUserId:'admin'});
    expect(new Headers(call[1]?.headers).get('If-Match')).toBe('"0"');
  });
  it('loads an imported file into the form for review before saving',async()=>{
    const transport=vi.fn<typeof fetch>().mockImplementation(async()=>new Response(JSON.stringify(initial),{status:200}));
    render(<ServerSettings client={new ControlClient('token',vi.fn(),transport)}/>);
    await screen.findByLabelText('Auto approve duration (days)');
    const file=new File(['ignored'],'settings.json',{type:'application/json'});Object.defineProperty(file,'text',{value:async()=>JSON.stringify({...initial,revision:9,autoApproveDevices:true,autoApproveDurationDays:7,autoApproveOwnerUserId:'owner'})});
    fireEvent.change(screen.getByLabelText('Server settings JSON file'),{target:{files:[file]}});
    await screen.findByText('Imported settings loaded. Review them, then save.');
    expect((screen.getByLabelText('Auto approve duration (days)') as HTMLInputElement).value).toBe('7');
    expect((screen.getByLabelText('Auto approve devices') as HTMLInputElement).checked).toBe(true);
    expect((screen.getByLabelText('Gateway name') as HTMLInputElement).value).toBe('ToolGate');
    expect(transport).toHaveBeenCalledTimes(1);
  });
});
