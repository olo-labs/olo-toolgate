# Copyright 2026 OLO Labs
# SPDX-License-Identifier: Apache-2.0
from datetime import timedelta
import unittest
from pathlib import Path
import yaml
from tools.ci.admission import decision, trigger_message, RESUME_AT


class AdmissionTests(unittest.TestCase):
    def test_every_external_workflow_is_behind_admission(self):
        root=Path(__file__).resolve().parents[2]
        for path in (root/'.github/workflows').glob('*.yml'):
            document=yaml.safe_load(path.read_text())
            events=document.get('on',document.get(True,{}))
            if not any(event in events for event in ('push','pull_request','workflow_dispatch','workflow_run','schedule','issue_comment')):
                continue
            jobs=document['jobs']
            if path.name=='manual-development-release.yml':
                # Explicit operator-only publishing is exempt; automatic workflows stay gated.
                self.assertEqual(set(events),{'workflow_dispatch'})
                for job in jobs.values():
                    self.assertEqual(job['if'],"github.event_name == 'workflow_dispatch' && github.ref == 'refs/heads/main'")
                self.assertNotIn('admission',jobs)
                continue
            self.assertEqual(jobs['admission']['uses'],'./.github/workflows/admission.yml')
            def gated(name,seen=None):
                if name=='admission':return True
                seen=(seen or set())|{name}
                needs=jobs[name].get('needs',[])
                if isinstance(needs,str):needs=[needs]
                return any(gated(parent,seen) for parent in needs if parent not in seen)
            for name,job in jobs.items():
                if name=='admission':continue
                with self.subTest(workflow=path.name,job=name):
                    self.assertTrue(gated(name),'Ungated job may consume quota')
                    if job.get('needs')=='admission':
                        self.assertIn("needs.admission.outputs.allow == 'true'",job['if'])

    def test_week_boundary_is_ist_midnight_and_never_bypassed_by_rc(self):
        self.assertFalse(decision('RC: build', RESUME_AT - timedelta(seconds=1))[0])
        self.assertTrue(decision('RC: build', RESUME_AT)[0])

    def test_manual_development_release_has_no_test_or_ci_dependencies(self):
        root=Path(__file__).resolve().parents[2]
        document=yaml.safe_load((root/'.github/workflows/manual-development-release.yml').read_text(encoding='utf-8'))
        self.assertEqual(set(document['jobs']),{'native','publish'})
        self.assertNotIn('needs',document['jobs']['native'])
        self.assertEqual(document['jobs']['publish']['needs'],'native')
        commands='\n'.join(step.get('run','') for job in document['jobs'].values() for step in job['steps'])
        for command in ('cargo test','cargo clippy','cargo fmt','--check','make check','pytest','unittest','tools/check.py','development.py prepare'):
            self.assertNotIn(command,commands)
        self.assertIn('-x test',commands)
        script=(root/'tools/release/manual_development.py').read_text(encoding='utf-8')
        self.assertIn("'--no-verify'",script)
        self.assertNotIn('workflow_runs',script)

    def test_disabled_and_non_rc_triggers_cannot_start_work(self):
        self.assertFalse(decision('RC: build', RESUME_AT, False)[0])
        for message in ('fix: build', 'rc: build', ' RC: build', '', None):
            self.assertFalse(decision(message, RESUME_AT)[0])

    def test_push_uses_head_commit_not_an_older_commit(self):
        event={'head_commit':{'message':'fix: current'},'commits':[{'message':'RC: previous'}]}
        self.assertEqual(trigger_message(event,'push',lambda sha: {}),'fix: current')

    def test_pr_requires_rc_title_and_real_head_commit(self):
        event={'pull_request':{'title':'RC: test','head':{'sha':'a'*40}}}
        message=trigger_message(event,'pull_request',lambda sha: {'message':'fix: head'})
        self.assertFalse(decision(message,RESUME_AT)[0])
        message=trigger_message(event,'pull_request',lambda sha: {'message':'RC: head'})
        self.assertTrue(decision(message,RESUME_AT)[0])

    def test_manual_and_release_events_cannot_bypass_prefix(self):
        self.assertEqual(trigger_message({'inputs':{'comment':'RC: manual'}},'workflow_dispatch',lambda sha: {}),'RC: manual')
        event={'workflow_run':{'event':'push','conclusion':'success','head_commit':{'message':'RC: release'}}}
        self.assertEqual(trigger_message(event,'workflow_run',lambda sha: {}),'RC: release')
        event['workflow_run']['conclusion']='cancelled'
        self.assertEqual(trigger_message(event,'workflow_run',lambda sha: {}),'')
