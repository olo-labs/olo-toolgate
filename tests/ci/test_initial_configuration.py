# Copyright 2026 OLO Labs
# SPDX-License-Identifier: Apache-2.0
import importlib.util
import json
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch
ROOT=Path(__file__).resolve().parents[2]
spec=importlib.util.spec_from_file_location('configuration',ROOT/'debug/configure-debug-agent.py')
module=importlib.util.module_from_spec(spec);spec.loader.exec_module(module)

class InitialConfigurationTests(unittest.TestCase):
    def test_release_bundle_has_complete_group_presets_and_no_individual_acl(self):
        bundle=module.load_configuration()
        for name,suffix in [('teams','Team'),('agentGroups','AgentGroup'),('toolGroups','ToolGroup'),('deviceGroups','DeviceGroup')]:
            records=bundle['records'][name]
            self.assertTrue({tier+suffix for tier in ('ReadOnly','ReadAndWrite','Admin')} <= {x['id'] for x in records})
            for record in records:
                members=next(record[key] for key in ('userIds','agentIds','toolIds','deviceIds') if key in record)
                self.assertEqual([],members)
        self.assertTrue(all(not b['allowedPackageDigests'] for b in bundle['records']['bindings']))
        self.assertFalse({'users','agents','devices','tools'}&bundle['records'].keys())

    def test_admin_credentials_use_login_api_without_docker_or_signing_keys(self):
        with patch.object(module.Configuration,'login') as login,patch.object(module.urllib.request,'build_opener'),patch.object(module,'docker_script') as docker,patch.dict(module.os.environ,{'TOOLGATE_ADMIN_USERNAME':'operator','TOOLGATE_ADMIN_PASSWORD':'example'},clear=True):
            module.Configuration()
            login.assert_called_once_with('admin','operator','example');docker.assert_not_called()

    def test_membership_allocation_uses_reviewed_api_and_expected_revision(self):
        config=object.__new__(module.Configuration)
        current=dict(entityType='AGENT',entityId='agent',groupIds=['default-agents'],revision=9)
        with patch.object(config,'api',side_effect=[current,dict(id='draft')]) as api,patch.object(config,'apply_change') as review:
            config.memberships('agents','agent',['ReadAndWriteAgentGroup'])
            self.assertEqual(api.call_args_list[1].args,('/api/control/v1/agents/agent/groups',{**current,'groupIds':['ReadAndWriteAgentGroup']},'PUT'))
            self.assertEqual(api.call_args_list[1].kwargs['revision'],9);review.assert_called_once_with(dict(id='draft'))

    def test_configured_membership_does_not_create_another_change(self):
        config=object.__new__(module.Configuration)
        with patch.object(config,'api',return_value=dict(groupIds=['ReadAndWriteAgentGroup'])) as api,patch.object(config,'apply_change') as review:
            config.memberships('agents','agent',['ReadAndWriteAgentGroup']);self.assertEqual(api.call_count,1);review.assert_not_called()
