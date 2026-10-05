// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
export const protocol = 1;
export function consoleOrigin(raw) {
  const url = new URL(raw);
  if (!url.pathname.startsWith('/console/') || url.username || url.password
    || !(url.protocol === 'https:' || (url.protocol === 'http:' && ['localhost','127.0.0.1'].includes(url.hostname)))) throw Error('Invalid console');
  return url.origin;
}
export function installer(document, target) {
  if (!document || typeof document.version !== 'string' || !Array.isArray(document.artifacts) || document.artifacts.length > 6) throw Error('Invalid release');
  const item = document.artifacts.find(a => a.platform === 'WINDOWS' && a.target === target);
  if (!item || item.filename !== `olo-toolgate-client-${document.version}-${target}.setup.exe`
    || !/^[0-9A-Za-z._-]+$/.test(document.version) || !/^[a-f0-9]{64}$/.test(item.sha256)
    || !Number.isSafeInteger(item.bytes) || item.bytes <= 0 || item.bytes > 104857600) throw Error('Invalid installer');
  return item;
}
