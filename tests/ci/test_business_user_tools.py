# Copyright 2026 OLO Labs; SPDX-License-Identifier: Apache-2.0
"""Public tool admission custody and authoritative outcomes; scripted transport."""
import json
from pathlib import Path
import tempfile
import unittest
from unittest.mock import Mock,patch

from bot.business import user_tools
from tools.admin.control import Refused


class UserToolsTests(unittest.TestCase):
    def setUp(self):
        self.client=Mock();self.tools=user_tools.UserTools(self.client,'reviewed-binding','reviewed-host')
        self.receipt=dict(invocationId='inv-owned',state='ADMITTED',requestKey='owned-request')

    def result(self,state='SUCCEEDED',finalized=True):
        return dict(record=dict(requestId='inv-owned',state='DONE'),
            operationOutcome=dict(state=state,finalized=finalized),
            result=dict(requestId='inv-owned',error=None,output=dict(json='{"items":[]}')))

    def test_begin_uses_one_public_user_admission_with_explicit_stable_key(self):
        self.client.request.return_value=(201,dict(invocation=dict(id='inv-owned',state='ADMITTED')))
        receipt=self.tools.begin('olo.sandbox/restart','sandbox.restart',dict(serviceId='demo',reference='business-42'),'owned-request')
        self.assertEqual('inv-owned',receipt['invocationId'])
        self.assertEqual('owned-request',self.client.request.call_args.args[-1])
        self.assertEqual('/api/control/v1/access/human/invocations',self.client.request.call_args.args[1])
        body=json.loads(self.client.request.call_args.args[2])
        self.assertEqual(dict(bindingId='reviewed-binding',deviceId='reviewed-host',request=dict(
            toolId='olo.sandbox/restart',action='sandbox.restart',arguments=dict(serviceId='demo',reference='business-42'))),body)
        self.client.request.assert_called_once()

    def test_only_finalized_matching_success_exposes_output(self):
        with patch.object(user_tools,'call',return_value=self.result()):
            self.assertEqual(dict(json='{"items":[]}'),self.tools.wait(self.receipt)['output'])
        for location in ('record','result'):
            value=self.result();value[location]['requestId']='another-invocation'
            with self.subTest(location=location),patch.object(user_tools,'call',return_value=value),self.assertRaises(Refused):
                self.tools.wait(self.receipt)
        for state in ('OUTCOME_UNKNOWN','RECONCILIATION_REQUIRED','FAILED','PARTIAL'):
            with self.subTest(state=state),patch.object(user_tools,'call',return_value=self.result(state,False)),self.assertRaises(user_tools.Unresolved):
                self.tools.wait(self.receipt)
        with patch.object(user_tools,'call',return_value=self.result(finalized=False)),self.assertRaises(user_tools.Unresolved):
            self.tools.wait(self.receipt,timeout=0)
        self.client.request.assert_not_called()

    def test_pending_approval_is_returned_without_polling_or_execution(self):
        with patch.object(user_tools,'call') as call:
            self.assertEqual('PENDING_APPROVAL',self.tools.wait({**self.receipt,'state':'PENDING_APPROVAL'})['state'])
        call.assert_not_called();self.client.request.assert_not_called()

    def test_cli_retains_admission_for_query_and_action_on_unknown_result(self):
        with tempfile.TemporaryDirectory() as temporary:
            for command in (['query','--kb-id','10000000-0000-4000-8000-000000000001','--query','approved policy'],
                            ['apply','--operation','restart','--subject','demo','--reference','business-42']):
                output=Path(temporary)/command[0]
                args=['user_tools','--origin','https://localhost','--token-file',str(Path(temporary)/'token'),
                    '--binding-id','reviewed-binding','--device-id','reviewed-host','--request-key','owned-request','--output',str(output)]+command
                with patch('sys.argv',args),patch.object(user_tools,'Control'),patch.object(user_tools.UserTools,'begin',return_value=self.receipt) as begin, \
                        patch.object(user_tools.UserTools,'wait',side_effect=user_tools.Unresolved('inv-owned','OUTCOME_UNKNOWN')):
                    with self.assertRaises(user_tools.Unresolved):user_tools.main()
                    self.assertEqual(self.receipt,json.loads(output.read_text())['admission'])
                    with self.assertRaises(Refused):user_tools.main()
                    begin.assert_called_once()

    def test_ambiguous_admission_retains_request_key_and_never_overwrites_prior_journal(self):
        with tempfile.TemporaryDirectory() as temporary:
            output=Path(temporary)/'journal.json'
            args=['user_tools','--origin','https://localhost','--token-file',str(Path(temporary)/'token'),
                '--binding-id','reviewed-binding','--device-id','reviewed-host','--request-key','owned-request','--output',str(output),
                'apply','--operation','provision','--subject','demo','--reference','business-42']
            with patch('sys.argv',args),patch.object(user_tools,'Control'),patch.object(user_tools.UserTools,'begin',side_effect=Refused('Transport outcome unknown')) as begin:
                with self.assertRaises(Refused):user_tools.main()
                before=output.read_bytes();self.assertEqual('owned-request',json.loads(before)['requestKey'])
                with self.assertRaises(Refused):user_tools.main()
                self.assertEqual(before,output.read_bytes());begin.assert_called_once()
