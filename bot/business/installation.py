# Copyright 2026 OLO Labs; SPDX-License-Identifier: Apache-2.0
"""Operator-reviewed processor connection and current Control grants; no bot executor."""
from copy import deepcopy
import hashlib
import ipaddress
import json
from pathlib import Path
import re

from tools.admin.control import Refused,read_file
from tools.admin.import_workflow import call


def connection(path):
    """Load destination-local trust and custody, which are never shipped in .olo."""
    path=Path(path)
    if not path.is_file():
        raise Refused('Provision the business processor and set OLO_BUSINESS_CONNECTION to its reviewed connection JSON; see bot/business/README.md')
    value=json.loads(read_file(path,65536))
    if (not isinstance(value,dict) or set(value)!={'schemaVersion','host','port','caFile','apiKeyFile','privateCidr','toolHostEvidenceFile'}
            or type(value['schemaVersion']) is not int or value['schemaVersion']!=1
            or value['host']!='business-processor' or type(value['port']) is not int or value['port']!=8443):
        raise Refused('Unsupported business processor connection')
    network=ipaddress.ip_network(value['privateCidr'],strict=True)
    if not network.is_private or network.is_loopback or network.is_link_local:
        raise Refused('Approve only the processor private deployment subnet')
    for key in ('caFile','apiKeyFile','toolHostEvidenceFile'):
        candidate=Path(value[key])
        if not candidate.is_absolute() or candidate.is_symlink():
            raise Refused('Processor custody paths must be absolute regular files')
    ca=read_file(value['caFile'],32768).decode('ascii')
    if '-----BEGIN CERTIFICATE-----' not in ca:
        raise Refused('Processor CA certificate required')
    secret=read_file(value['apiKeyFile'],513).decode('ascii').strip()
    if not re.fullmatch('[A-Za-z0-9_-]{32,512}',secret):
        raise Refused('Use a generated private processor API key')
    return {**value,'ca':ca,'secret':secret}


def host_evidence(value,tenant,host_ids):
    """Read operator-observed public identity; never infer trust from a host name."""
    evidence=json.loads(read_file(value['toolHostEvidenceFile'],16384))
    if (not isinstance(evidence,dict) or set(evidence)!={'hostId','tenantId','userCode','keyFingerprint'}
            or evidence['tenantId']!=tenant or host_ids!=[evidence['hostId']]
            or not isinstance(evidence['hostId'],str) or not re.fullmatch('[A-Za-z0-9][A-Za-z0-9._-]{0,127}',evidence['hostId'])
            or not isinstance(evidence['keyFingerprint'],str) or not re.fullmatch('[a-f0-9]{64}',evidence['keyFingerprint'])
            or evidence['userCode'] is not None and (not isinstance(evidence['userCode'],str)
                or not re.fullmatch('[A-Za-z0-9]{1,128}',evidence['userCode']))):
        raise Refused('Tool Host public evidence differs from the reviewed installation')
    return evidence


def authorize(client,profile,value):
    snapshot=call(client,'/api/control/v1/config/export')
    tenant=snapshot['tenantId']
    if not re.fullmatch('[A-Za-z0-9][A-Za-z0-9._-]{0,127}',tenant):
        raise Refused('Invalid verified installation tenant')
    # This implementation exposes only the two explicit synthetic intakes of
    # the selected starter. Production intakes need separately reviewed grants.
    name=profile['profileId']
    if not re.fullmatch('starter-(ocr|medical|vlsic|loan|invoice|support|itsm|onboarding)',name):
        raise Refused('Unsupported processor starter')
    template=next(g for g in snapshot['grants'] if g['id']==name+'-processor-capability')
    resources=[dict(kind='CUSTOM',locator='documents://'+tenant+'/intake/'+name+'-'+suffix,match='EXACT') for suffix in ('sample','exception')]
    resources.append(dict(kind='CUSTOM',locator='secret://businessprocessorapikey',match='EXACT'))
    missing=[]
    for purpose in ('CAPABILITY','SERVICE','SECRET'):
        grant=deepcopy(template)
        grant.update(id=name+'-intake-'+purpose.lower(),name=name+' authorized sample intake '+purpose.lower(),purpose=purpose,revision=1)
        grant['scope']['resources']=resources if purpose!='SECRET' else resources[-1:]
        existing=next((g for g in snapshot['grants'] if g['id']==grant['id']),None)
        if existing is None:
            missing.append(grant)
        elif {**existing,'revision':1}!=grant:
            raise Refused('Existing intake grant differs; review its current scope before setup')
    existing=call(client,'/api/control/v1/vault')['items']
    if any(item['name']=='businessprocessorapikey' and (item['toolGroupId']!='business-processor-tools'
           or item['deviceGroupId']!='business-processor-hosts') for item in existing):
        raise Refused('Existing processor secret has a different group scope; custody was preserved')
    for grant in missing:
        call(client,'/api/control/v1/grants',grant)
    if not any(item['name']=='businessprocessorapikey' and item['toolGroupId']=='business-processor-tools'
               and item['deviceGroupId']=='business-processor-hosts' for item in existing):
        call(client,'/api/control/v1/vault',dict(name='businessprocessorapikey',value=value['secret'],
            toolGroupId='business-processor-tools',deviceGroupId='business-processor-hosts'))


def deploy_processor(client,profile,value):
    pin=next(item for item in profile['packages'] if item['packageId']=='olo.business')
    tenant=call(client,'/api/control/v1/config/export')['tenantId']
    policies=[]
    for route in ('submit','result'):
        policies.append(dict(routeId=route,classification='WRITE',consentRequired=False,acknowledgedUnfencedRisk=False,
            fencedTarget=dict(targetId='document-jobs',targetClass='SHARED_SQL'),
            intent=dict(path='/v1/'+route,query={},headers={},contentType='application/json',bodyMode='CANONICAL_JSON',
                body=dict(tenantId=tenant,useCase={'$arg':'/useCase'},intakeId={'$arg':'/intakeId'}))))
    return call(client,'/api/control/v1/packages/deployments',dict(packageDigest=pin['packageDigest'],
        globals={'ProcessorHost':value['host']},hostIds=pin['hostIds'],
        privateRanges=[dict(host=value['host'],cidr=value['privateCidr'],ports=[value['port']])],
        destinationTls={'processor':dict(caBundle=value['ca'],spkiPins=[])},effectPolicies=policies))


def stage_intake(folder,prefix,output):
    """Copy SHA-pinned authored samples into a fresh read-only repository folder."""
    manifest=json.loads((folder/(prefix+'-intake.json')).read_text(encoding='utf-8'))
    identifier=(folder/(prefix+'-input.txt')).read_text(encoding='utf-8').strip()
    if not re.fullmatch('[A-Za-z0-9_-]{1,96}',identifier):raise Refused('Invalid authored intake ID')
    destination=Path(output)/identifier
    if destination.exists():raise Refused('Intake already exists; choose a fresh output directory')
    files={}
    for document in manifest['documents']:
        name=document['file'];path=folder/name
        if name=='manifest.json' or path.name!=name or path.is_symlink() or not path.resolve().is_relative_to(folder.resolve()):
            raise Refused('Unsafe authored source')
        raw=read_file(path,2*1024*1024)
        if len(raw)!=document['bytes'] or 'sha256:'+hashlib.sha256(raw).hexdigest()!=document['sha256']:
            raise Refused('Authored intake source differs from its manifest')
        files[name]=raw
    destination.mkdir(parents=True,mode=0o700)
    for name,raw in files.items():(destination/name).write_bytes(raw)
    (destination/'manifest.json').write_text(json.dumps(manifest,separators=(',',':'))+'\n',encoding='utf-8')
    return identifier
