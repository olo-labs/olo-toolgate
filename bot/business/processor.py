# Copyright 2026 OLO Labs; SPDX-License-Identifier: Apache-2.0
"""Read-only domain processing behind a governed tool route, never an executor."""
import json
import re

from bot.business.documents import intake, fields
from bot.business.rules import completeness, finding, reconcile, required_fields, structural_netlist, valid_date, invoice_from_sources

SUPPORTED = {'ocr','medical','vlsic','loan','invoice','support','itsm','onboarding'}


def process(use_case, request):
    if use_case not in SUPPORTED or not isinstance(request,dict):
        raise ValueError('Unsupported business request')
    if use_case=='invoice':
        allowed={'invoice','invoiceDocumentId','documents','purchaseOrder','receipts','quantityTolerance'}
        if not set(request)<=allowed or ('invoice' in request)==('invoiceDocumentId' in request):
            raise ValueError('Supply exactly one structured invoice or invoice document identifier')
        sources=intake(request.get('documents',[]))
        if 'invoiceDocumentId' in request:
            invoice,findings,locations=invoice_from_sources(sources,request['invoiceDocumentId'])
        else:
            invoice=request['invoice'];findings=[];locations={}
        if invoice is not None:
            findings=reconcile(invoice,request['purchaseOrder'],request['receipts'],request.get('quantityTolerance','0'))
            for item in findings:
                if locations:
                    item['evidence']=[e for e in item['evidence'] if e['sourceId']!=invoice['id']]+locations[item['checkId']]
        return result(report(findings,'Invoice, purchase order and receipt comparison'),sources)
    if not set(request)<={'documents','fieldSchema','requiredDocuments','netlistDocumentId','checklist'}:
        raise ValueError('Unknown domain request field')
    sources=intake(request['documents'])
    schema=request.get('fieldSchema',[])
    if not isinstance(schema,list) or len(schema)>32:
        raise ValueError('Field schema limit exceeded')
    ids=[]
    for item in schema:
        if (not isinstance(item,dict) or not set(item)<={'id','labels','required','format'}
                or not {'id','labels'}<=set(item) or not isinstance(item['id'],str)
                or not re.fullmatch('[A-Za-z][A-Za-z0-9_-]{0,63}',item['id'])
                or not isinstance(item['labels'],list) or not 1<=len(item['labels'])<=8
                or item.get('format') not in (None,'email','date')
                or ('required' in item and type(item['required']) is not bool)
                or any(not isinstance(label,str) or not 1<=len(label)<=80 for label in item['labels'])):
            raise ValueError('Invalid field schema')
        ids.append(item['id'])
    if len(ids)!=len(set(ids)):
        raise ValueError('Duplicate field IDs')
    findings=required_fields(sources,schema)
    required=request.get('requiredDocuments',[])
    if (not isinstance(required,list) or len(required)>16
            or any(not isinstance(value,str) or not re.fullmatch('[A-Za-z0-9][A-Za-z0-9_.-]{0,95}',value) for value in required)
            or len(set(required))!=len(required)):
        raise ValueError('Invalid required-document list')
    findings+=completeness(sources,required)
    if use_case=='vlsic':
        findings+=structural_netlist(sources,request['netlistDocumentId'],request['checklist'])
    elif use_case=='medical':
        # Chronology contains only explicit valid dates, never clinical inference.
        events=[]
        for source in sources:
            for line in source.text.splitlines():
                for date in re.findall(r'\b\d{4}-\d{2}-\d{2}\b',line):
                    if valid_date(date):
                        events.append((date,source.evidence(line)))
        if events:
            events.sort(key=lambda item:(item[0],item[1]['sourceId'],item[1]['line']))
            findings.append(finding('chronology','pass','Explicit dated document events; professional interpretation required',
                                    [item[0] for item in events],[item[1] for item in events]))
        else:
            findings.append(finding('chronology','needs review','No valid dated source events supplied'))
    elif use_case in ('support','itsm','onboarding'):
        # This tool inventories supplied sources. RAG queries and memory access
        # must be performed with the existing user-bound platform contracts.
        # A document claiming to be approved cannot authorize an action.
        findings.append(finding('knowledgeProvenance','needs review',
            'Verify governed retrieval receipts and published source versions before relying on policy interpretation'))
    if not findings:
        findings.append(finding('configuration','needs review','No review rules were configured'))
    title={'ocr':'Document extraction','medical':'Medical document administration',
           'vlsic':'Structural design checklist','loan':'Loan document completeness and consistency',
           'support':'Support source inventory','itsm':'Service desk source inventory',
           'onboarding':'Joining document inventory'}[use_case]
    return result(report(findings,title),sources)


def result(report,sources):
    return dict(report=report,sources=[dict(sourceId=s.id,documentId=s.document_id,page=s.page,text=s.text,
                sha256=s.sha256,engine=s.engine,rotation=s.rotation) for s in sources])


def report(findings,title):
    findings=[{**item,'evidence':[normalize_evidence(e) for e in item['evidence']]} for item in findings]
    return dict(summary=title,findings=findings,draft='',
                nextActions=[item['checkId']+': '+item['detail'] for item in findings if item['status']!='pass']
                or ['Reviewer to confirm findings against the submitted evidence.'])


def normalize_evidence(item):
    return {**dict(sourceId='',documentId='',page=0,line=0,quote='',sha256='',engine='structured-record',rotation=0,pointer=''),**item}
