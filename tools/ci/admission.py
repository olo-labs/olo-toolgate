# Copyright 2026 OLO Labs
# SPDX-License-Identifier: Apache-2.0
"""Fail-closed admission for quota-controlled workflow execution."""
from datetime import datetime, timezone
import json
import os
from pathlib import Path
import re
import urllib.request

# Monday October 12, 00:00 IST. No build/release work before this boundary.
RESUME_AT = datetime(2026, 10, 11, 18, 30, tzinfo=timezone.utc)


def decision(message, now, enabled=True):
    if not enabled:
        return False, 'CI automation is paused'
    if now < RESUME_AT:
        return False, 'CI is paused through October 11 (IST)'
    if not isinstance(message, str) or not message.startswith('RC:'):
        return False, 'Trigger comment must start with RC:'
    return True, 'RC: trigger admitted'


def trigger_message(event, event_name, fetch_commit):
    if event_name == 'push':
        return (event.get('head_commit') or {}).get('message', '')
    if event_name == 'workflow_dispatch':
        return (event.get('inputs') or {}).get('comment', '')
    if event_name == 'pull_request':
        pr = event.get('pull_request') or {}
        if not pr.get('title', '').startswith('RC:'):
            return ''
        return fetch_commit(pr.get('head', {}).get('sha', '')).get('message', '')
    if event_name == 'workflow_run':
        run = event.get('workflow_run') or {}
        if run.get('conclusion') != 'success' or run.get('event') != 'push':
            return ''
        return (run.get('head_commit') or {}).get('message', '')
    return ''


def main():
    event = json.loads(Path(os.environ['GITHUB_EVENT_PATH']).read_text())
    now = datetime.now(timezone.utc)
    enabled = os.environ.get('CI_ENABLED') == 'true'
    allowed, reason = decision('RC:', now, enabled)
    if allowed:
        def fetch_commit(sha):
            if not re.fullmatch(r'[a-f0-9]{40}', sha):
                return {}
            url = f"https://api.github.com/repos/{os.environ['GITHUB_REPOSITORY']}/commits/{sha}"
            request = urllib.request.Request(url, headers={
                'Authorization': 'Bearer ' + os.environ['GITHUB_TOKEN'],
                'Accept': 'application/vnd.github+json',
            })
            with urllib.request.urlopen(request, timeout=20) as response:
                return json.load(response).get('commit', {})
        message = trigger_message(event, os.environ['GITHUB_EVENT_NAME'], fetch_commit)
        allowed, reason = decision(message, now, enabled)
    with open(os.environ['GITHUB_OUTPUT'], 'a', encoding='utf-8') as output:
        output.write('allow=' + str(allowed).lower() + '\n')
    print(reason)


if __name__ == '__main__':
    main()
