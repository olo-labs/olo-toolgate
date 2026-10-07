// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
import { spawnSync } from 'node:child_process';

// Git runs hooks from the checkout root. Test the same UI files being committed.
const paths = ['apps/admin-ui', 'apps/browser-extension', 'packages/contracts/typescript', 'package.json', 'package-lock.json', 'tools/ci', '.githooks'];
const unstaged = spawnSync('git', ['diff', '--quiet', '--', ...paths]);
if (unstaged.status !== 0) {
  console.error('Stage your UI/check changes before committing so the checks verify the committed code.');
  process.exit(1);
}
const extensionTests=spawnSync(process.execPath,['--test','apps/browser-extension/tests/protocol.mjs','apps/browser-extension/tests/worker.mjs'],{stdio:'inherit'});
if(extensionTests.status!==0)process.exit(extensionTests.status??1);
const secretScan=spawnSync(process.execPath,['tools/ci/secret-scan.mjs'],{stdio:'inherit'});
if(secretScan.status!==0)process.exit(secretScan.status??1);
const npm = process.platform === 'win32' ? 'npm.cmd' : 'npm';
for (const args of [['run','ui:check'], ['--workspace','@olo-labs/toolgate-admin-ui','test']]) {
  const result = spawnSync(npm,args,{stdio:'inherit',shell:process.platform==='win32'});
  if (result.error) console.error(result.error.message);
  if (result.status !== 0) process.exit(result.status ?? 1);
}
const browser=spawnSync(process.execPath,['tools/ci/browser-downloads.mjs'],{stdio:'inherit'});
if(browser.status!==0)process.exit(browser.status??1);
