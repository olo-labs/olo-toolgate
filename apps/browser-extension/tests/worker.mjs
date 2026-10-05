// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
import {beforeEach,test} from 'node:test';
import assert from 'node:assert/strict';
let listener,state={},nativeCalls=0,cancelled=[],download;
const id='emmemldedebhbloibichmmdlbpjakfkf',origin='https://gate.example';
globalThis.chrome={
  runtime:{id,onMessage:{addListener(fn){listener=fn;}},getManifest(){return {version_name:'0.10.0-dev',version:'0.10.0.23'};},async sendNativeMessage(){nativeCalls++;throw Error('Host unavailable');}},
  storage:{local:{async get(key){return {[key]:state[key]};},async remove(key){delete state[key];}},session:{async set(){}}},
  action:{async openPopup(){}},
  downloads:{async search(){return download?[download]:[];},async cancel(value){cancelled.push(value);}}
};
await import('../worker.js');
beforeEach(()=>{state={};nativeCalls=0;cancelled=[];download=undefined;});
function request(operation,url=origin+'/console/#enroll'){
  return new Promise(resolve=>listener({operation},{tab:{id:2,url}},resolve));
}
test('unapproved origins cannot probe local IPC',async()=>{
  const result=await request('status');assert.equal(result.phase,'approval');assert.equal(nativeCalls,0);
  assert.equal((await request('status','https://gate.example/other')).error,'INVALID_ORIGIN');
});
test('native host absence remains unavailable, not connected',async()=>{
  state[origin]={approved:true};const result=await request('status');assert.equal(nativeCalls,1);assert.equal(result.client,null);
});
test('progress survives a worker restart and tracks this extension download',async()=>{
  state[origin]={approved:true,downloadId:42,userCode:'ABCDEF0123456789'};download={id:42,byExtensionId:id,state:'complete'};
  const result=await request('status');assert.equal(result.phase,'open-installer');assert.equal(result.userCode,'ABCDEF0123456789');
});
test('cancel only touches this extension active download and clears approval',async()=>{
  state[origin]={approved:true,downloadId:42};download={id:42,byExtensionId:id,state:'in_progress'};
  await request('cancel');assert.deepEqual(cancelled,[42]);assert.equal(state[origin],undefined);
  state[origin]={approved:true,downloadId:43};download={id:43,byExtensionId:'another-extension',state:'in_progress'};
  await request('cancel');assert.deepEqual(cancelled,[42]);
});
