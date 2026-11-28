# Copyright 2026 OLO Labs; SPDX-License-Identifier: Apache-2.0
"""Execute actual starter graphs with scripted adapters, not real model quality."""
from collections import deque
from copy import deepcopy
from dataclasses import replace
import json
from pathlib import Path
import unittest

from toolgate_runtime.runner import Agent, Candidate, Denied, Runner, Tool, ToolOutcome
from test_runner import Gate, lease

ROOT=Path(__file__).resolve().parents[3]
import sys
sys.path.insert(0,str(ROOT))
from tests.ci.starter_fixture import sample_result


class StarterGraphTests(unittest.TestCase):
    def run_sample(self,folder,exception=False,deny=False,bad_report=False):
        profile=json.loads((folder/'bot.json').read_text(encoding='utf-8'))
        config=profile['workflows'];workflow=config['workflows'][0]
        expected=sample_result(folder,exception)
        text=expected['submittedInput'];report=expected['report']
        bindings={a['agentId']:a for a in workflow['agents']}
        agents=tuple(Agent(a['agentId'],a['modelGroupId'],a['systemPrompt'],
            entry_eligible=bindings[a['agentId']]['entryEligible'],delegation_allowed=bindings[a['agentId']]['delegationAllowed'])
            for a in config['agentDefinitions'])
        run=replace(lease(folder.name),message=text,graph=workflow['graph'],agents=agents,
            remediation_mode=workflow['manualRunMode'])
        gate=Gate(run);gate.catalogs={};gate.calls=[]
        gate.catalogs[agents[0].agent_id]=(Tool('olo.business/process-intake',False),)
        gate.tool_outcome=ToolOutcome('SUCCEEDED',dict(processing=dict(report=deepcopy(report),sources=expected['sources']),
            intakeId=text,manifestSha256=expected['manifestSha256']))
        gate.agent_catalog=tuple(Candidate(a.agent_id) for a in agents)
        for a in agents:gate.decisions[a.agent_id]=deque([dict(kind='finish',result=deepcopy(expected['analysis']))])
        if bad_report:gate.decisions[agents[-1].agent_id][0]['result']['findings']=[dict(status='verified')]
        if deny:
            def refuse(*args,**kwargs):raise Denied('private authority detail')
            gate.authorize_step=refuse
        result=Runner(gate).execute(run)
        return result,gate,text,report

    def test_all_eight_execute_both_stages_preserving_original_input_and_review_output(self):
        count=0
        for folder in (ROOT/'bot/configurations').glob('starter-*'):
            count+=1
            for exception in (False,True):
                with self.subTest(bot=folder.name,exception=exception):
                    result,gate,text,report=self.run_sample(folder,exception)
                    self.assertEqual('SUCCEEDED',result.state,result.code)
                    self.assertEqual(report,result.result['report'])
                    self.assertEqual(text,result.result['submittedInput'])
                    self.assertTrue(result.result['humanReviewRequired'])
                    self.assertEqual([(folder.name+'-analyst','olo.business/process-intake',dict(useCase=folder.name.removeprefix('starter-'),intakeId=text))],gate.calls)
                    self.assertEqual(2,len(gate.inferences))
                    received=gate.inferences[1][3][0]['content']
                    self.assertEqual(report,received['evidence']['report'])
                    self.assertNotIn('findings',received['candidate'])
        self.assertEqual(8,count)

    def test_denied_authority_cannot_infer_or_claim_a_successful_run(self):
        for key in ('itsm','onboarding'):
            result,gate,_,_=self.run_sample(ROOT/'bot/configurations'/('starter-'+key),deny=True)
            self.assertNotEqual('SUCCEEDED',result.state)
            self.assertEqual([],gate.calls)
            self.assertEqual([],gate.inferences)
            self.assertNotIn('private authority detail',str(result))

    def test_invalid_finding_status_fails_the_actual_graph_output_schema(self):
        result,gate,_,_=self.run_sample(ROOT/'bot/configurations/starter-loan',bad_report=True)
        self.assertNotEqual('SUCCEEDED',result.state)
        self.assertEqual(1,len(gate.calls))

    def test_unknown_tool_outcome_stops_before_model_interpretation_without_retry(self):
        folder=ROOT/'bot/configurations/starter-itsm'
        original=Gate.call_tool;calls=[]
        def uncertain(gate,*args,**kwargs):
            calls.append(True)
            return ToolOutcome('OUTCOME_UNKNOWN',None)
        from unittest.mock import patch
        with patch.object(Gate,'call_tool',uncertain):
            result,gate,_,_=self.run_sample(folder)
        self.assertNotEqual('SUCCEEDED',result.state)
        self.assertEqual([True],calls)
        self.assertEqual([],gate.inferences)
