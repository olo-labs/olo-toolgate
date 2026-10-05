// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
import {test} from 'node:test';
import assert from 'node:assert/strict';
// .js sources are ESM within this package.
import {consoleOrigin,installer} from '../protocol.js';
test('only TLS consoles and explicit loopback development origins',()=>{
  assert.equal(consoleOrigin('https://gate.example/console/#enroll'),'https://gate.example');
  assert.equal(consoleOrigin('http://127.0.0.1:18089/console/'),'http://127.0.0.1:18089');
  for(const raw of ['http://gate.example/console/','https://u:p@gate.example/console/','file:///console/','https://gate.example/other'])assert.throws(()=>consoleOrigin(raw));
});
test('installer target, filename, checksum and size must all match',()=>{
  const target='x86_64-pc-windows-msvc';
  const item={platform:'WINDOWS',target,filename:`olo-toolgate-client-0.10.0-dev-${target}.setup.exe`,sha256:'a'.repeat(64),bytes:30};
  const document={version:'0.10.0-dev',artifacts:[item]};
  assert.equal(installer(document,target),item);
  for(const change of [{filename:'../evil.exe'},{sha256:'x'.repeat(64)},{bytes:104857601},{bytes:0},{bytes:1.5},{platform:'LINUX'},{target:'aarch64-pc-windows-msvc'}])assert.throws(()=>installer({...document,artifacts:[{...item,...change}]},target));
});
