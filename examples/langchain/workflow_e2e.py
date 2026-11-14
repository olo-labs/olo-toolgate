# Copyright 2026 OLO Labs
# SPDX-License-Identifier: Apache-2.0
"""Qualify one real trigger, published workflow, agent, model, and Gateway tool."""
import json
import os
import secrets
import subprocess
import sys
import time

import manage
import trigger_agent
import trigger_e2e


COMPOSE = trigger_e2e.COMPOSE + ['-f', str(manage.HERE / 'compose.workflow.yaml')]
WORKFLOW = 'hotfolder-intake'
AGENT = 'langchain-linux-agent'
PROFILE = 'hotfolder-workflow'
SUBSCRIPTION = 'hotfolder-workflow-agent'


def provision(config):
    trigger_agent.ensure_subscription(config.api, int(time.time() * 1000), name=SUBSCRIPTION)
    assignment = config.api('/api/control/v1/triggers/assignments/' + trigger_agent.ASSIGNMENT)['metadata']['uid']
    subscription = config.api('/api/control/v1/triggers/subscriptions/' + SUBSCRIPTION)['metadata']['uid']
    group = next(group for group in config.all('agent-groups') if group['id'] == 'langchain-linux-agents')
    role = 'hotfolder-workflow-runtime'
    config.upsert('roles', dict(id=role, name='Workflow service role', revision=1, enabled=True,
                                portalRole='BASIC', roleType='ACTOR_SERVICE', managementRules=[]))
    config.upsert('agent-groups', {**group, 'roleIds': sorted(set(group['roleIds']) | {role})})
    scope = config.api('/api/control/v1/grants/standard-ReadAndWrite-capability')['scope']
    scope = {**scope, 'toolGroups': dict(ids=[], all=True), 'deviceGroups': dict(ids=[], all=True),
             'allActions': False, 'actions': ['workflow.execute', 'model.infer'],
             'conditions': {**scope['conditions'], 'requireOnline': False, 'highRisk': False},
             'resources': [dict(kind='CUSTOM', match='EXACT', locator=locator)
                           for locator in ('workflow-group:hotfolder', 'model-group:hotfolder-models')]}
    config.upsert('grants', dict(id=role, name='Hot-folder workflow service', revision=1, enabled=True,
                                 sourceType='ROLE', sourceId=role, purpose='SERVICE', scope=scope))
    secret = (manage.STATE / 'workflow/model/model.token').read_text(encoding='ascii').strip()
    config.api('/api/control/v1/vault', dict(name='hotfolder-model-key', value=secret,
        toolGroupId='default-tools', deviceGroupId='default-devices'))
    document = dict(profileVersion=2, profileId=PROFILE, packages=[], workflows=dict(
        workflowGroups=[dict(id='hotfolder', displayName='Hot-folder')],
        modelGroups=[dict(id='hotfolder-models', deploymentIds=['intake'])],
        modelDeployments=[dict(id='intake', origin='https://workflow-model:8443', modelId='intake-model',
            secretName='hotfolder-model-key', toolGroupId='default-tools', deviceGroupId='default-devices')],
        agentDefinitions=[dict(agentId=AGENT, modelGroupId='hotfolder-models',
            systemPrompt='After receiving the folder listing from the context tool, finish with {"kind":"finish","result":{"agent":"hotfolder-intake","reviewed":true}}.',
            contextSources=[dict(toolId='examples.hotfolder/list', arguments={'folder': 'inbox'})], outputSchema={})],
        workflows=[dict(id=WORKFLOW, versionId='v1', workflowGroupId='hotfolder', modelGroupId='hotfolder-models',
            systemPrompt=('On the first turn, return {"kind":"delegate","agentId":"langchain-linux-agent","input":"Inspect the inbox"}. '
                          'After receiving the agent result, return {"kind":"finish","result":{"reviewed":true}}.'),
            contextSources=[], parameterSchema={}, resultSchema={}, limits=dict(maxSteps=8, maxHandoffs=1, maxDurationSeconds=90),
            agents=[dict(agentId=AGENT, agentGroupId=group['id'], entryEligible=True, delegationAllowed=False)])],
        dispatchBindings=[dict(id='hotfolder-dispatch', subscriptionUid=subscription, sourceAssignmentUid=assignment,
            workflowId=WORKFLOW, workflowVersionId='v1', serviceAgentId=AGENT, serviceAgentGroupId=group['id'],
            messagePointer='/name', remediationMode='PREPARE')]))
    base = '/api/control/v1/workflow-profiles'
    staged = config.api(base, document)
    digest = {'profileDigest': staged['profileDigest']}
    config.api(f'{base}/{PROFILE}/review', digest)
    config.api(f'{base}/{PROFILE}/publish', {**digest, 'expectedGeneration': staged['generation']})
    return subscription


def run():
    manage.load_environment()
    config = manage.Configuration(manage.wait_ready(), True)
    trigger_e2e.running(config)
    subscription = provision(config)
    listener_token = config.tokens['admin']
    custody = manage.STATE / 'workflow/worker'
    custody.mkdir(parents=True, exist_ok=True)
    ca = (manage.STATE / 'agents/gateway-ca.crt').read_text(encoding='ascii')
    manage.shared.private_file(custody / 'gateway-ca.crt', ca)
    # The isolated Quickstart identity provider issues the same role a deployment IdP would issue.
    worker_token = manage.docker('quickstart', ['/opt/quickstart-python/bin/python', '-c',
        "import sys;sys.path.insert(0,'/opt/quickstart');import supervisor as s;"
        "print(s.jwt(['toolgate-workflow-runtime'],'workflow-e2e',directory_bound=False))"])
    manage.shared.private_file(custody / 'worker.token', worker_token)
    control = 'https://localhost:' + os.environ.get('TOOLGATE_TLS_PORT', '18451')
    name = 'workflow-' + secrets.token_hex(6) + '.txt'
    trigger_e2e.device(f"umask 077 && printf '%s' 'workflow acceptance' > {trigger_e2e.FOLDER}/inbox/{name}")
    from toolgate_runtime.trigger_bridge import Subscription
    from toolgate_runtime.trigger_transport import ControlTriggers, TriggerListener
    transport = ControlTriggers(control, lambda: listener_token, ca_file=str(custody / 'gateway-ca.crt'))
    _, receipt = TriggerListener(transport, Subscription(SUBSCRIPTION, subscription)).listen_once(max_events=1, idle_timeout=180)
    if receipt is None or receipt.disposition != 'ADMITTED':
        raise ValueError('The real trigger did not admit a workflow')
    print(f'Trigger admitted run {receipt.run_id}', flush=True)
    worker = subprocess.run(COMPOSE + ['run', '--rm', '-T', '--no-deps', 'workflow-worker'], cwd=manage.HERE,
                            capture_output=True, text=True, timeout=140)
    try:
        result = json.loads(worker.stdout.strip().splitlines()[-1])
    except (IndexError, ValueError):
        raise ValueError(f'Workflow worker returned no result (exit {worker.returncode})') from None
    if worker.returncode != 0 or (result.get('runId'), result.get('workflowId'), result.get('state')) != (receipt.run_id, WORKFLOW, 'SUCCEEDED'):
        raise ValueError('Workflow did not complete: ' + json.dumps(result))
    print(json.dumps(result, sort_keys=True))


if __name__ == '__main__':
    try:
        run()
    except (ValueError, OSError, subprocess.SubprocessError) as failure:
        print(f'FAILED: {failure}', file=sys.stderr)
        raise SystemExit(1)
