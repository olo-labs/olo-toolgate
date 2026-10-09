# Copyright 2026 OLO Labs
# SPDX-License-Identifier: Apache-2.0
"""Explicit migration planning and read-only shadow evidence. Never applies grants."""
import argparse
import hashlib
import json
import sys
from pathlib import Path
import urllib.request
sys.path.insert(0,str(Path(__file__).parent))
from recovery import canonical,read,write

COLLECTIONS=('users','teams','agents','tools','policies','devices','roles','deviceGroups','agentGroups','toolGroups','grants','delegations','agentDelegations','bindings','extractors','workloadBindings','identityBindings','deviceEvidence')

def prepare(archive,baseline,plan):
    if baseline.get('formatVersion')!=2 or baseline['tenantId']!=archive['tenantId'] or plan['tenantId']!=baseline['tenantId'] or plan['revision']!=baseline['revision']:
        raise ValueError('Exact post-cutover group graph revision and tenant required')
    if set(plan)-{'tenantId','revision','records','reasonDigest'} or not isinstance(plan.get('records'),dict) or len(plan.get('reasonDigest',''))!=64:
        raise ValueError('Explicit bounded reviewed records and reason digest required')
    proposed=json.loads(json.dumps(baseline))
    for collection,records in plan['records'].items():
        if collection not in COLLECTIONS or not isinstance(records,list):raise ValueError('Canonical collection required')
        previous={r['id']:r for r in baseline[collection]}
        for record in records:
            if not isinstance(record,dict) or not record.get('id') or any(key in record for key in ('access','allowedToolIds','deviceGroupId') if collection in ('users','agents','tools')):
                raise ValueError('Individual permissions are retired')
            if collection=='identityBindings' and record['id'] in previous:
                for field in ('userId','issuer','subject'):
                    if record[field]!=previous[record['id']][field]:raise ValueError('Stable identity facts cannot change')
            if record['id'] in previous and collection in ('users','agents','devices') and record['revision']<previous[record['id']]['revision']:
                raise ValueError('Identity revision rollback rejected')
        if len({r['id'] for r in records})!=len(records):raise ValueError('Duplicate mapping')
        proposed[collection]=sorted({**previous,**{r['id']:r for r in records}}.values(),key=lambda r:r['id'])
    # Missing old fields are evidence only; they are never interpreted as ANY or all=true.
    preserved={name:sorted(r['id'] for r in baseline[name]) for name in ('users','agents','tools','devices')}
    evidence=dict(formatVersion=1,tenantId=baseline['tenantId'],cutoverRevision=baseline['revision'],
                  archiveDigest=hashlib.sha256(canonical(archive)).hexdigest(),reasonDigest=plan['reasonDigest'],
                  proposedSnapshotDigest=hashlib.sha256(canonical(proposed)).hexdigest(),preservedIdentifiers=preserved,
                  effectsAuthorized=False,requiresIndependentReviews=2)
    return proposed,evidence

def shadow(origin,token,snapshot,cases):
    if not origin.startswith('https://') and not origin.startswith(('http://127.0.0.1:','http://localhost:')):raise ValueError('Verified HTTPS or explicit local evaluation origin required')
    if not 1<=len(cases)<=100:raise ValueError('One to 100 captured decision cases required')
    reports=[]
    for case in cases:
        evidence=case['legacyEvidence']
        body=dict(snapshot=snapshot,evaluation=case['evaluation'],observedLegacyDecision=case['decision'],legacyEvidenceDigest=hashlib.sha256(canonical(evidence)).hexdigest())
        req=urllib.request.Request(origin.rstrip('/')+'/api/control/v1/access/shadow',canonical(body),{'Authorization':'Bearer '+token,'Content-Type':'application/json'},method='POST')
        with urllib.request.urlopen(req,timeout=15) as response:reports.append(json.load(response))
    return dict(formatVersion=1,effectsAuthorized=False,cases=reports,accessExpansions=sum(r['accessExpansion'] for r in reports))

def main():
    parser=argparse.ArgumentParser(description=__doc__);commands=parser.add_subparsers(dest='command',required=True)
    p=commands.add_parser('prepare');p.add_argument('--archive',required=True);p.add_argument('--baseline',required=True);p.add_argument('--plan',required=True);p.add_argument('--snapshot-output',required=True);p.add_argument('--evidence-output',required=True)
    p=commands.add_parser('shadow');p.add_argument('--origin',required=True);p.add_argument('--token-file',required=True);p.add_argument('--snapshot',required=True);p.add_argument('--cases',required=True);p.add_argument('--output',required=True)
    args=parser.parse_args()
    if args.command=='prepare':
        snapshot,evidence=prepare(read(args.archive),read(args.baseline),read(args.plan));write(args.snapshot_output,snapshot);write(args.evidence_output,evidence)
    else:
        path=Path(args.token_file);token=path.read_text().strip()
        if path.is_symlink() or path.stat().st_size>16384 or not token:raise ValueError('Bounded protected credential file required')
        write(args.output,shadow(args.origin,token,read(args.snapshot),read(args.cases)))
    print('Read-only migration evidence written. Submit the complete snapshot through reviewed configuration import after examining every expansion.')
if __name__=='__main__':main()
