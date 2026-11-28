# Copyright 2026 OLO Labs; SPDX-License-Identifier: Apache-2.0
"""Synthetic archive/result fixtures for builder tests, never executable packages."""
from copy import deepcopy
import hashlib
import io
import json
from pathlib import Path
import zipfile

from bot.bundle import encoded,package

ROOT=Path(__file__).resolve().parents[2]


def business_package(*args,**kwargs):
    definition=json.loads((ROOT/'bot/configurations/starter-loan/referenced-config.json').read_text())['tools'][0]['definition']
    descriptor=dict(toolId=definition['id'],description=definition['description'],
                    inputSchema=definition['inputSchema'],outputSchema=definition['outputSchema'],
                    authorization=dict(action='document.process'),effect=dict(safety='NON_IDEMPOTENT'),reconciliation='MANUAL')
    manifest=dict(schemaVersion=2,id='olo.business',version='0.1.0',tools=[dict(descriptor=descriptor,
        toolDescriptorDigest='sha256:'+hashlib.sha256(encoded(descriptor)).hexdigest())])
    payload=encoded(manifest);output=io.BytesIO()
    with zipfile.ZipFile(output,'w',zipfile.ZIP_DEFLATED) as archive:
        archive.writestr('toolgate-package.json',payload)
        archive.writestr('CHECKSUMS.sha256',hashlib.sha256(payload).hexdigest()+'  toolgate-package.json\n')
    raw=output.getvalue();item,manifest=package(raw)
    return item,raw,manifest


def sample_result(folder,exception=False):
    prefix='exception' if exception else 'sample'
    report=json.loads((folder/('expected-exception-report.json' if exception else 'expected-report.json')).read_text())
    intake=json.loads((folder/(prefix+'-intake.json')).read_text())
    documents={d['id']:d for d in intake['documents']};sources={}
    # Quotes come from the authored test report. Live parser acceptance uses
    # actual documents; this helper tests graph routing and result immutability.
    for finding in report['findings']:
        for evidence in finding['evidence']:
            if evidence['pointer']:continue
            key=evidence['sourceId'];document=documents[evidence['documentId']]
            source=sources.setdefault(key,dict(sourceId=key,documentId=evidence['documentId'],page=evidence['page'],
                text='',sha256=document['sha256'],engine=evidence['engine'],rotation=evidence['rotation']))
            lines=source['text'].splitlines()
            while len(lines)<evidence['line']:lines.append('')
            lines[evidence['line']-1]=evidence['quote'];source['text']='\n'.join(lines)
            evidence['sha256']=document['sha256']
    commentary=dict(summary='Review the supplied findings.',draft='',nextActions=['Resolve outstanding evidence with the reviewer.'],citations=[])
    return dict(useCase=folder.name.removeprefix('starter-'),mode='deterministic-evidence-with-agent-review',
                humanReviewRequired=True,submittedInput=(folder/(prefix+'-input.txt')).read_text(),
                report=report,sources=list(sources.values()),manifestSha256='sha256:'+'a'*64,
                analysis=deepcopy(commentary),review=deepcopy(commentary))
