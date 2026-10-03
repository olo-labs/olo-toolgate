# Copyright 2026 OLO Labs
# SPDX-License-Identifier: Apache-2.0
"""Approval Helm gates: external permit keys, bounded behavior and explicit trust."""
import json
import sys
from pathlib import Path
import yaml

ROOT=Path(__file__).resolve().parents[2]
sys.path.insert(0,str(ROOT/'tools'))
from check import run


def checks():
    chart='deploy/helm/olo-toolgate'
    base=['--set','gateway.enabled=true','--set','gateway.credentialsSecret=runtime',
          '--set','gateway.bundle.enabled=true','--set','gateway.bundle.keyringSecret=policy-keys',
          '--set','gateway.bundle.tokenSecret=policy-access',
          '--set','gateway.networkPolicy.controlTo[0].podSelector.matchLabels.role=control-tls',
          '--set','gateway.networkPolicy.dnsTo[0].namespaceSelector.matchLabels.kubernetes\\.io/metadata\\.name=kube-system',
          '--set','gateway.approval.enabled=true','--set','gateway.approval.signingSecret=permit-signing',
          '--set','gateway.approval.tokenSecret=approval-access','--set','control.enabled=true',
          '--set','control.publicKeySecret=identity','--set','control.database.credentialsSecret=db',
          '--set','control.database.caSecret=db-ca','--set','control.bundle.enabled=true',
          '--set','control.bundle.signingSecret=policy-signing','--set','control.approval.enabled=true']
    folder=ROOT/'build/approval/render';folder.mkdir(parents=True,exist_ok=True)
    for label,extra in [('install',[]),('upgrade',['--is-upgrade']),('rotation',['--set','gateway.approval.keyRevision=rotation-2'])]:
        run(['helm','lint',chart,*base,*[e for e in extra if e!='--is-upgrade']])
        rendered=run(['helm','template','approval',chart,*base,*extra],capture=True).stdout
        docs=[d for d in yaml.safe_load_all(rendered) if d]
        config=next(d for d in docs if d['kind']=='ConfigMap' and 'gateway.json' in d.get('data',{}))
        gateway=json.loads(config['data']['gateway.json'])
        assert 'policy' not in gateway and not gateway['approval']['developmentLoopbackHttp']
        assert gateway['approval']['privateKeyPath']=='/etc/toolgate/permit-signing/private.pem'
        assert gateway['approval']['permitLifetimeMs']<=10000
        control=next(d for d in docs if d['kind']=='ConfigMap' and 'TOOLGATE_CONTROL_APPROVAL_ENABLED' in d.get('data',{}))
        assert control['data']['TOOLGATE_CONTROL_APPROVAL_ENABLED']=='true'
        for deployment in (d for d in docs if d['kind']=='Deployment'):
            pod=deployment['spec']['template']['spec'];volumes={v['name']:v for v in pod['volumes']}
            if deployment['metadata']['name'].endswith('gateway'):
                assert volumes['permit-signing']['secret']['secretName']=='permit-signing'
                assert volumes['permit-signing']['secret']['defaultMode']==0o440
                assert volumes['approval-auth']['secret']['secretName']=='approval-access'
                assert 'bundle-signing' not in volumes
            else:
                assert 'permit-signing' not in volumes
        path=folder/(label+'.yaml');path.write_text(rendered,encoding='utf-8',newline='\n')
        run(['docker','run','--rm','-v',f'{ROOT.as_posix()}:/work:ro','ghcr.io/yannh/kubeconform:v0.6.7',
             '-strict','-summary','-kubernetes-version','1.32.0',f'/work/{path.relative_to(ROOT).as_posix()}'])
    negatives=[['gateway.approval.signingSecret='],['gateway.approval.tokenSecret='],['gateway.bundle.enabled=false'],
               ['control.bundle.enabled=false'],['gateway.approval.url=http://control'],['gateway.approval.permitLifetimeMs=10001'],
               ['gateway.approval.privateKey=forbidden'],['gateway.approval.requestTimeoutMs=2000'],['control.approval.maxActive=10001']]
    for values in negatives:
        extra=[part for value in values for part in ('--set',value)]
        run(['helm','template','invalid',chart,*base,*extra],expect_failure=True)
    print('Approval chart install/upgrade/rotation and nine negative gates passed')


if __name__=='__main__':checks()
