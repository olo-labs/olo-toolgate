# Copyright 2026 OLO Labs
# SPDX-License-Identifier: Apache-2.0
import contextlib
import importlib.util
import io
from pathlib import Path
import unittest
ROOT=Path(__file__).resolve().parents[2]
spec=importlib.util.spec_from_file_location('agent_mimic',ROOT/'debug/agent-mimic.py')
mimic=importlib.util.module_from_spec(spec);spec.loader.exec_module(mimic)

class ScriptTests(unittest.TestCase):
    def test_discovers_before_sequential_effects_and_prints_one_real_log_entry(self):
        calls=[]
        class Agent:
            def rpc(self,method,params=None):
                calls.append((method,params))
                if method=='server/discover':return {'supportedVersions':[mimic.VERSION]}
                if method=='tools/list':return {'tools':[{'name':'hotfolder.write_text'},{'name':'client.read_log_entry'}]}
                if params['name']=='hotfolder.write_text':return {'structuredContent':{'success':True}}
                return {'structuredContent':{'file':'packets.jsonl','entry':{'direction':'RECEIVE','requestId':'mcp-123'}}}
        output=io.StringIO()
        with contextlib.redirect_stdout(output):entry=mimic.mimic(Agent(),'rahul-nigam.txt')
        self.assertEqual([call[0] for call in calls],['server/discover','tools/list','tools/call','tools/call'])
        self.assertEqual(calls[2][1]['arguments'],{'path':'rahul-nigam.txt','text':'My Name is Rahul Nigam'})
        self.assertEqual(calls[3][1],{'name':'client.read_log_entry','arguments':{}})
        self.assertEqual(entry['requestId'],'mcp-123')
        self.assertEqual(output.getvalue().count('"direction"'),1)
    def test_unavailable_catalog_causes_no_effect(self):
        calls=[]
        class Agent:
            def rpc(self,method,params=None):
                calls.append(method)
                return {'supportedVersions':[mimic.VERSION]} if method=='server/discover' else {'tools':[]}
        with contextlib.redirect_stdout(io.StringIO()),self.assertRaises(ValueError):mimic.mimic(Agent(),'rahul-nigam.txt')
        self.assertEqual(calls,['server/discover','tools/list'])
    def test_failed_write_does_not_issue_second_call(self):
        calls=[]
        class Agent:
            def rpc(self,method,params=None):
                calls.append(method)
                if method=='server/discover':return {'supportedVersions':[mimic.VERSION]}
                if method=='tools/list':return {'tools':[{'name':'hotfolder.write_text'},{'name':'client.read_log_entry'}]}
                return {'structuredContent':{'success':False}}
        with contextlib.redirect_stdout(io.StringIO()),self.assertRaises(ValueError):mimic.mimic(Agent(),'rahul-nigam.txt')
        self.assertEqual(calls,['server/discover','tools/list','tools/call'])

if __name__=='__main__':unittest.main()
