// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// Reproduce the CI OS/browser environment from the Git index on Windows hosts.
import { spawnSync } from 'node:child_process';
import { mkdtempSync, rmSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join, resolve, dirname, basename } from 'node:path';

const work=mkdtempSync(join(tmpdir(),'toolgate-linux-browser-'));
try {
  const tree=spawnSync('git',['write-tree'],{encoding:'utf8'});
  if(tree.status!==0)throw Error('Cannot read the staged source for Linux verification');
  const archive=join(work,'source.tar');
  const checkout=spawnSync('git',['archive','--format=tar',`--output=${archive}`,tree.stdout.trim()],{stdio:'inherit'});
  if(checkout.status!==0)throw Error('Cannot export the staged source for Linux verification');
  const result=spawnSync('docker',['run','--rm','--init','-v',`${archive.replaceAll('\\','/')}:/source.tar:ro`,
    'mcr.microsoft.com/playwright:v1.63.0-noble','sh','-eu','-c',
    'mkdir -p /work; tar -xf /source.tar -C /work; cd /work; npm ci --ignore-scripts; node tools/ci/browser-downloads.mjs'],{stdio:'inherit'});
  if(result.status!==0)throw Error(`Linux browser gate failed: ${result.status}. Docker must be running.`);
} finally {
  // Verify the target before recursively removing our own temporary snapshot.
  if(dirname(resolve(work))!==resolve(tmpdir())||!basename(work).startsWith('toolgate-linux-browser-'))throw Error('Unsafe cleanup path');
  rmSync(work,{recursive:true,force:true});
}
