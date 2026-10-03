// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
import { ApiError } from './api';

/** Present only bounded correlation identifiers and our own human-safe messages. */
export function Failure({ error, retry }: { error: unknown; retry?: () => void }) {
  const failure = error instanceof ApiError ? error : new ApiError(0, 'NETWORK');
  return <div className="notice error" role="alert"><strong>{failure.message}</strong>
    {failure.requestId && <small>Reference: {failure.requestId}</small>}
    {retry && <button onClick={retry}>Try again</button>}</div>;
}
