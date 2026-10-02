# Copyright 2026 OLO Labs
# SPDX-License-Identifier: Apache-2.0
"""Signed-mode chart gates: external key mounts, fixed trust and intentional egress."""
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
          '--set','gateway.bundle.enabled=true','--set','gateway.bundle.keyringSecret=bundle-keys',
          '--set','gateway.bundle.tokenSecret=bundle-access',
          '--set','gateway.networkPolicy.controlTo[0].podSelector.matchLabels.role=control-tls',
          '--set','gateway.networkPolicy.dnsTo[0].namespaceSelector.matchLabels.kubernetes\\.io/metadata\\.name=kube-system',
          '--set','control.enabled=true','--set','control.publicKeySecret=identity','--set','control.database.credentialsSecret=db',
          '--set','control.database.caSecret=db-ca','--set','control.bundle.enabled=true','--set','control.bundle.signingSecret=policy-signing']
    folder=ROOT/'build/policy/render';folder.mkdir(parents=True,exist_ok=True)
    for extra in ([],['--is-upgrade'],['--set','gateway.bundle.trustRevision=rotation-2','--set','control.bundle.keyRevision=rotation-2']):
        rendered=run(['helm','template','policy',chart,*base,*extra],capture=True).stdout
        docs=[d for d in yaml.safe_load_all(rendered) if d]
        gateway=next(d for d in docs if d['kind']=='ConfigMap' and 'gateway.json' in d.get('data',{}))
        config=json.loads(gateway['data']['gateway.json']);assert 'policy' not in config
        assert config['bundleSource']['keyringPath']=='/etc/toolgate/bundle-keys/keyring.json'
        assert not config['bundleSource']['developmentLoopbackHttp']
        deployments=[d for d in docs if d['kind']=='Deployment'];assert len(deployments)==2
        for d in deployments:
            pod=d['spec']['template']['spec'];assert pod['securityContext']['runAsNonRoot']
            assert pod['containers'][0]['securityContext']['readOnlyRootFilesystem']
            volumes={v['name']:v for v in pod['volumes']}
            if d['metadata']['name'].endswith('gateway'):
                assert volumes['bundle-keys']['secret']['secretName']=='bundle-keys'
                assert volumes['bundle-auth']['secret']['secretName']=='bundle-access'
                assert 'bundle-signing' not in volumes
            else:
                assert volumes['bundle-signing']['secret']['secretName']=='policy-signing'
                assert volumes['bundle-signing']['secret']['defaultMode']==0o440
        policy=next(d for d in docs if d['kind']=='NetworkPolicy' and d['metadata']['name'].endswith('gateway'))
        assert len(policy['spec']['egress'])==2
        path=folder/('upgrade.yaml' if '--is-upgrade' in extra else 'rotation.yaml' if extra else 'install.yaml')
        path.write_text(rendered,encoding='utf-8',newline='\n')
        run(['docker','run','--rm','-v',f'{ROOT.as_posix()}:/work:ro','ghcr.io/yannh/kubeconform:v0.6.7',
             '-strict','-summary','-kubernetes-version','1.32.0',f'/work/{path.relative_to(ROOT).as_posix()}'])
    for extra in [ ['--set','gateway.bundle.keyringSecret='],['--set','gateway.bundle.tokenSecret='],['--set','control.bundle.signingSecret='],
                   ['--set','gateway.bundle.url=http://control/api/control/v1/bundles/current'],['--set','gateway.bundle.maxGraceMs=300001'],
                   ['--set','gateway.networkPolicy.controlTo=null'],['--set','gateway.networkPolicy.dnsTo=null'],['--set','control.bundle.privateKey=forbidden'] ]:
        run(['helm','template','invalid',chart,*base,*extra],expect_failure=True)
    print('Signed bundle Helm install/upgrade/rotation, secret mounts, explicit egress and negative gates passed')


if __name__=='__main__':checks()
