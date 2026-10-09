# Copyright 2026 OLO Labs
# SPDX-License-Identifier: Apache-2.0
"""Exercise the real LangChain loop with a scripted model, without paid inference.

Run in the built agent image: docker compose run --rm -T --no-deps
  --entrypoint python -v ./test_agent.py:/app/test_agent.py:ro agent /app/test_agent.py
"""
import contextlib
import io
import json
import unittest

from langchain_core.language_models.chat_models import BaseChatModel
from langchain_core.messages import AIMessage
from langchain_core.outputs import ChatGeneration, ChatResult

import agent


class Gateway:
    def __init__(self):
        self.files = {'support-tickets.json': '{"id":"DEMO-101"}'}
        self.calls = []
        self.success = True
        self.corrupt_write = False
        self.omit_tool = None

    def rpc(self, method, params=None):
        self.calls.append((method, params))
        if method == 'tools/list':
            tools = []
            for name in agent.TOOLS | {'admin.unrelated'}:
                if name == self.omit_tool:
                    continue
                properties = {}
                if name in {'hotfolder.read_text', 'hotfolder.write_text'}:
                    properties['path'] = {'type': 'string'}
                if name == 'hotfolder.write_text':
                    properties['text'] = {'type': 'string'}
                tools.append(dict(name=name, description='Example ' + name, inputSchema={
                    'type': 'object', 'properties': properties,
                    'required': list(properties), 'additionalProperties': False}))
            return {'tools': tools}
        name, arguments = params['name'], params['arguments']
        if name == 'hotfolder.list':
            result = {'paths': list(self.files)}
        elif name == 'hotfolder.read_text':
            result = {'text': self.files[arguments['path']]}
        elif name == 'hotfolder.write_text':
            if self.success:
                self.files[arguments['path']] = 'wrong' if self.corrupt_write else arguments['text']
            result = {'success': self.success}
        elif name == 'client.read_log_entry':
            result = {'file': 'packets.jsonl', 'entry': {'direction': 'OUT'}}
        else:
            raise AssertionError('Unrelated tool must not execute')
        return {'structuredContent': result}


class ScriptedModel(BaseChatModel):
    responses: list[AIMessage]
    cursor: int = 0

    @property
    def _llm_type(self):
        return 'scripted-example-test'

    def bind_tools(self, tools, **kwargs):
        return self

    def _generate(self, messages, stop=None, run_manager=None, **kwargs):
        reply = self.responses[self.cursor]
        self.cursor += 1
        return ChatResult(generations=[ChatGeneration(message=reply)])


def tool_message(name, arguments):
    return AIMessage(content='', tool_calls=[dict(name=name, args=arguments, id=name)])


def model():
    return ScriptedModel(responses=[
        tool_message('hotfolder_list', {}),
        tool_message('hotfolder_read_text', {'path': 'support-tickets.json'}),
        tool_message('hotfolder_write_text', {'path': 'support-triage.md',
                                            'text': '# Fictional support triage\nDEMO-101: investigate.'}),
        tool_message('hotfolder_read_text', {'path': 'support-triage.md'}),
        AIMessage(content='Report written and checked.'),
    ])


class AgentTests(unittest.TestCase):
    def test_real_langchain_loop_dispatches_through_gateway_and_verifies_report(self):
        gateway = Gateway()
        with contextlib.redirect_stdout(io.StringIO()) as output:
            self.assertEqual(agent.run(gateway, 'tickets', model()), 'support-triage.md')
        self.assertIn('Verified report', output.getvalue())
        self.assertEqual([params['name'] for method, params in gateway.calls if method == 'tools/call'],
                         ['hotfolder.list', 'hotfolder.read_text', 'hotfolder.write_text',
                          'hotfolder.read_text', 'hotfolder.read_text'])

    def test_scope_rejects_unrelated_reads_and_writes_before_gateway(self):
        gateway = Gateway()
        tools = agent.tools_for(gateway, {'support-tickets.json'}, 'support-triage.md')
        self.assertEqual(set(tools), agent.TOOLS)
        with self.assertRaisesRegex(ValueError, 'reads only'):
            tools['hotfolder.read_text'].invoke({'path': 'private.txt'})
        with self.assertRaisesRegex(ValueError, 'writes only'):
            tools['hotfolder.write_text'].invoke({'path': 'private.txt', 'text': 'overwrite'})
        self.assertEqual(len(gateway.calls), 1)

    def test_missing_current_grant_stops_before_model_or_effects(self):
        gateway = Gateway()
        gateway.omit_tool = 'hotfolder.write_text'
        with self.assertRaisesRegex(ValueError, 'target must expose'):
            agent.run(gateway, 'tickets', model())
        self.assertEqual(len(gateway.calls), 1)

    def test_fabricated_completion_is_rejected(self):
        gateway = Gateway()
        fake = ScriptedModel(responses=[AIMessage(content='I wrote the report.')])
        with self.assertRaisesRegex(ValueError, 'did not write'):
            agent.run(gateway, 'tickets', fake)
        self.assertNotIn('support-triage.md', gateway.files)

    def test_readback_mismatch_is_rejected(self):
        gateway = Gateway()
        gateway.corrupt_write = True
        with self.assertRaisesRegex(ValueError, 'did not match'):
            agent.run(gateway, 'tickets', model())

    def test_failed_native_write_is_not_recorded_as_success(self):
        gateway = Gateway()
        gateway.success = False
        writes = []
        tools = agent.tools_for(gateway, writes=writes)
        with self.assertRaisesRegex(ValueError, 'did not confirm'):
            tools['hotfolder.write_text'].invoke({'path': 'report.md', 'text': 'data'})
        self.assertEqual(writes, [])

    def test_smoke_exercises_device_operations_without_model(self):
        gateway = Gateway()
        with contextlib.redirect_stdout(io.StringIO()) as output:
            agent.smoke(gateway)
        self.assertIn('No model was called', output.getvalue())
        self.assertIn('langchain-smoke.txt', gateway.files)


if __name__ == '__main__':
    unittest.main()
