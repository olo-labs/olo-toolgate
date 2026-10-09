# Copyright 2026 OLO Labs
# SPDX-License-Identifier: Apache-2.0
import contextlib
import importlib.util
import io
import json
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch
ROOT=Path(__file__).resolve().parents[2]
spec=importlib.util.spec_from_file_location('agent_mimic',ROOT/'debug/agent-mimic.py')
mimic=importlib.util.module_from_spec(spec);spec.loader.exec_module(mimic)

class ScriptTests(unittest.TestCase):
    def test_repeats_only_existing_write_then_reads_log(self):
        calls=[]
        class Agent:
            def rpc(self,method,params=None):
                calls.append((method,params))
                if params['name']=='hotfolder.write_text':return {'structuredContent':{'success':True}}
                return {'structuredContent':{'file':'packets.jsonl','entry':{'direction':'RECEIVE'}}}
        with contextlib.redirect_stdout(io.StringIO()),patch.object(mimic.time,'monotonic',side_effect=[0,1,2,8]),patch.object(mimic.time,'sleep') as sleep:
            mimic.mimic(Agent(),'rahul-nigam.txt',8)
        self.assertEqual([params['name'] for _,params in calls],['hotfolder.write_text','hotfolder.write_text','client.read_log_entry'])
        self.assertEqual(calls[0],calls[1])
        sleep.assert_called_once_with(0.25)
    def test_repeated_write_failure_stops_without_reading_log(self):
        calls=[]
        class Agent:
            def rpc(self,method,params=None):
                calls.append(params['name'])
                return {'structuredContent':{'success':len(calls)==1}}
        with contextlib.redirect_stdout(io.StringIO()),patch.object(mimic.time,'monotonic',side_effect=[0,1,2]),patch.object(mimic.time,'sleep'),self.assertRaises(ValueError):
            mimic.mimic(Agent(),'rahul-nigam.txt',8)
        self.assertEqual(calls,['hotfolder.write_text','hotfolder.write_text'])
    def test_invalid_duration_sends_nothing(self):
        class Agent:
            def rpc(self,*args):raise AssertionError('Invalid duration must not call the gateway')
        for duration in (-1,61,True,1.5,'8'):
            with self.subTest(duration=duration),self.assertRaisesRegex(ValueError,'Duration seconds'):
                mimic.mimic(Agent(),'rahul-nigam.txt',duration)
    def test_call_that_outlasts_interval_is_not_repeated(self):
        calls=[]
        class Agent:
            def rpc(self,method,params=None):
                calls.append(params['name'])
                return {'structuredContent':{'success':True}} if len(calls)==1 else {'structuredContent':{'file':'packets.jsonl','entry':{}}}
        with contextlib.redirect_stdout(io.StringIO()),patch.object(mimic.time,'monotonic',side_effect=[0,20]),patch.object(mimic.time,'sleep') as sleep:
            mimic.mimic(Agent(),'rahul-nigam.txt',8)
        self.assertEqual(calls,['hotfolder.write_text','client.read_log_entry'])
        sleep.assert_not_called()
    def test_only_calls_two_tools_and_prints_their_responses(self):
        calls=[]
        class Agent:
            def rpc(self,method,params=None):
                calls.append((method,params))
                if params['name']=='hotfolder.write_text':return {'structuredContent':{'success':True}}
                return {'structuredContent':{'file':'packets.jsonl','entry':{'direction':'RECEIVE','requestId':'mcp-123'}}}
        output=io.StringIO()
        with contextlib.redirect_stdout(output):entry=mimic.mimic(Agent(),'rahul-nigam.txt')
        self.assertEqual([call[0] for call in calls],['tools/call','tools/call'])
        self.assertEqual(calls[0][1],{'name':'hotfolder.write_text','arguments':{'path':'rahul-nigam.txt','text':'My Name is Rahul Nigam'}})
        self.assertEqual(calls[1][1],{'name':'client.read_log_entry','arguments':{}})
        self.assertEqual(entry['requestId'],'mcp-123')
        self.assertEqual(output.getvalue().count('"direction"'),1)
        self.assertIn('Client file-write response:',output.getvalue())
        self.assertIn('"success": true',output.getvalue())
        self.assertIn('Client log response:',output.getvalue())
        self.assertIn('"file": "packets.jsonl"',output.getvalue())
    def test_rejected_call_stops_without_setup_or_second_call(self):
        calls=[]
        class Agent:
            def rpc(self,method,params=None):
                calls.append(method)
                raise ValueError('MCP HTTP 401: UNAUTHORIZED')
        with contextlib.redirect_stdout(io.StringIO()),self.assertRaises(ValueError):mimic.mimic(Agent(),'rahul-nigam.txt')
        self.assertEqual(calls,['tools/call'])
    def test_failed_write_does_not_issue_second_call(self):
        calls=[]
        class Agent:
            def rpc(self,method,params=None):
                calls.append(method)
                return {'structuredContent':{'success':False}}
        with contextlib.redirect_stdout(io.StringIO()),self.assertRaises(ValueError):mimic.mimic(Agent(),'rahul-nigam.txt')
        self.assertEqual(calls,['tools/call'])
    def test_uses_configured_trust_instead_of_an_exported_debug_ca(self):
        with tempfile.TemporaryDirectory() as directory:
            root=Path(directory)
            stale=root/'.dev/debug/toolgate-quickstart-ca.crt'
            stale.parent.mkdir(parents=True);stale.write_text('stale')
            config=root/'client.json'
            ca=root/'configured-ca.crt'
            config.write_text(json.dumps({'serverUrl':'https://localhost:18450','caCertificatePath':str(ca)}))
            with patch.object(mimic,'ROOT',root),patch.object(mimic,'installed_client_config',return_value=config):
                self.assertEqual(mimic.resolve_ca('https://localhost:18450'),ca)
                config.unlink()
                self.assertIsNone(mimic.resolve_ca('https://localhost:18450'))

if __name__=='__main__':unittest.main()
