# Copyright 2026 OLO Labs; SPDX-License-Identifier: Apache-2.0
"""Deterministic document/checklist findings, separated from model interpretation."""
from decimal import Decimal, InvalidOperation
import re

from bot.business.documents import fields


def finding(check_id,status,reason,values=(),evidence=()):
    return dict(checkId=check_id,status=status,detail=reason,values=list(values),evidence=list(evidence))


def required_fields(sources,schema):
    extracted=fields(sources,{item['id']:item['labels'] for item in schema})
    findings=[]
    for item in schema:
        observed=extracted[item['id']];values=list(dict.fromkeys(v['value'] for v in observed if v['value']))
        evidence=[v['evidence'] for v in observed]
        if not values:
            status='needs review' if item.get('required',True) else 'unsupported'
            reason='Required field missing or unreadable' if status=='needs review' else 'Optional field not supplied'
        elif len(values)>1:
            status='fail';reason='Conflicting source values; preserve each for reviewer resolution'
        elif item.get('format')=='email' and not re.fullmatch(r'[^\s@]+@[^\s@]+\.[^\s@]+',values[0]):
            status='needs review';reason='Email syntax invalid; candidate retained in evidence; do not infer a replacement';values=[]
        elif item.get('format')=='date' and not valid_date(values[0]):
            status='needs review';reason='Date must be a valid ISO calendar date; candidate retained in evidence';values=[]
        else:
            status='pass';reason='Value extracted from the supplied source'
        findings.append(finding(item['id'],status,reason,values,evidence))
    return findings


def valid_date(value):
    from datetime import date
    try:
        return re.fullmatch(r'\d{4}-\d{2}-\d{2}',value) is not None and date.fromisoformat(value) is not None
    except ValueError:
        return False


def completeness(sources,required):
    ids={source.document_id for source in sources}
    return [finding('document:'+name,'pass' if name in ids else 'needs review',
                    'Document supplied' if name in ids else 'Required document absent',
                    [name] if name in ids else [],
                    [s.evidence(s.text.splitlines()[0]) for s in sources
                     if s.document_id==name and s.text.splitlines()]) for name in required]


def structural_netlist(sources,document_id,checklist):
    if checklist.get('version')!='structural-verilog-1':
        raise ValueError('Unsupported checklist version')
    source=next((s for s in sources if s.document_id==document_id),None)
    if source is None:
        raise ValueError('Netlist source missing')
    text=re.sub(r'//[^\n]*','',source.text)
    # Intentionally narrow ANSI-port structural dialect. Unsupported syntax is
    # rejected rather than partially parsed into a passing report.
    match=re.fullmatch(r'\s*module\s+([A-Za-z_]\w*)\s*\((.*?)\)\s*;\s*(.*?)\s*endmodule\s*',text,re.S)
    if not match:
        raise ValueError('Malformed or unsupported structural netlist')
    ports={};signals=set();assignments=[]
    for declaration in match[2].split(','):
        port=re.fullmatch(r'\s*(input|output|inout)\s+([A-Za-z_]\w*)\s*',declaration)
        if not port or port[2] in ports:
            raise ValueError('Unsupported or duplicate port declaration')
        ports[port[2]]=port[1]
    for statement in match[3].split(';'):
        if not statement.strip():
            continue
        wire=re.fullmatch(r'\s*wire\s+([A-Za-z_]\w*)\s*',statement)
        assign=re.fullmatch(r'\s*assign\s+([A-Za-z_]\w*)\s*=\s*([A-Za-z_]\w*|[01])\s*',statement)
        if wire:
            if wire[1] in signals or wire[1] in ports:
                raise ValueError('Duplicate signal declaration')
            signals.add(wire[1])
        elif assign:
            assignments.append((assign[1],assign[2]))
        else:
            raise ValueError('Unsupported structural statement')
    declared=set(ports)|signals
    if any(target not in declared or value not in declared|{'0','1'} for target,value in assignments):
        raise ValueError('Assignment references an undeclared structural signal')
    findings=[]
    for rule in checklist['rules']:
        kind=rule['kind'];name=rule.get('name','')
        if kind=='port':
            passed=name in ports and ports[name]==rule['direction']
        elif kind=='signal':
            passed=name in signals
        elif kind=='module':
            passed=name==match[1]
        elif kind=='document-port':
            design=[item for item in sources if item.document_id==rule['referenceDocumentId']]
            observed=fields(design,{'port':[rule['label']]})['port']
            values=list(dict.fromkeys(item['value'] for item in observed if item['value']))
            if len(values)!=1:
                findings.append(finding(rule['id'],'needs review','Document interface name missing or conflicting',
                    values,[item['evidence'] for item in observed]));continue
            name=values[0];passed=name in ports
            findings.append(finding(rule['id'],'pass' if passed else 'fail',
                '[structural-verilog-1] Document interface matches netlist' if passed else '[structural-verilog-1] Document/netlist naming conflict',
                [name]+sorted(ports),[item['evidence'] for item in observed]+[source.evidence(source.text.splitlines()[0])]))
            continue
        else:
            findings.append(finding(rule['id'],'unsupported','Rule is outside structural-verilog-1'))
            continue
        findings.append(finding(rule['id'],'pass' if passed else 'fail',
            'Structural requirement present' if passed else 'Required structure absent or different',
            [name] if passed else [],[source.evidence(source.text)]))
    return findings


def money(value):
    if not isinstance(value,str) or not re.fullmatch(r'\d{1,12}(?:\.\d{1,4})?',value):
        raise ValueError('Use nonnegative bounded decimal strings')
    return Decimal(value)


def reconcile(invoice,purchase_order,receipts,tolerance='0'):
    tolerance=money(tolerance)
    for document in (invoice,purchase_order):
        if not isinstance(document,dict) or set(document)!={'id','purchaseOrderId','currency','lines'}:
            raise ValueError('Invalid financial document envelope')
        if any(not isinstance(document[key],str) or not re.fullmatch('[A-Za-z0-9_-]{1,96}',document[key]) for key in ('id','purchaseOrderId')):
            raise ValueError('Invalid financial matching identifier')
        if not isinstance(document['currency'],str) or not re.fullmatch('[A-Z]{3}',document['currency']):
            raise ValueError('Currency must be an uppercase three-letter code')
        if not isinstance(document['lines'],list) or not 1<=len(document['lines'])<=100:
            raise ValueError('Supply one to one hundred financial lines')
        for line in document['lines']:
            if (not isinstance(line,dict) or set(line)!={'sku','quantity','unitPrice'}
                    or not isinstance(line['sku'],str) or not re.fullmatch('[A-Za-z0-9_-]{1,64}',line['sku'])):
                raise ValueError('Invalid financial line')
            money(line['quantity']);money(line['unitPrice'])
        if len({line['sku'] for line in document['lines']})!=len(document['lines']):
            raise ValueError('Oversized or duplicate invoice/PO lines')
    if invoice['purchaseOrderId']!=purchase_order['purchaseOrderId']:
        raise ValueError('Invoice/PO matching key mismatch')
    if invoice['currency']!=purchase_order['currency']:
        raise ValueError('Currency conversion is unsupported; currencies must match')
    ordered={line['sku']:line for line in purchase_order['lines']}
    received={}
    receipt_ids=set()
    if not isinstance(receipts,list) or len(receipts)>100:
        raise ValueError('Receipt limit exceeded')
    for receipt in receipts:
        if (not isinstance(receipt,dict) or set(receipt)!={'id','purchaseOrderId','lines'}
                or not isinstance(receipt['id'],str) or not re.fullmatch('[A-Za-z0-9_-]{1,96}',receipt['id'])
                or not isinstance(receipt['lines'],list) or not 1<=len(receipt['lines'])<=100):
            raise ValueError('Invalid receipt')
        if receipt['id'] in receipt_ids or receipt['purchaseOrderId']!=invoice['purchaseOrderId']:
            raise ValueError('Duplicate or foreign receipt')
        receipt_ids.add(receipt['id'])
        skus=set()
        for line in receipt['lines']:
            if (not isinstance(line,dict) or set(line)!={'sku','quantity'} or not isinstance(line['sku'],str)
                    or not re.fullmatch('[A-Za-z0-9_-]{1,64}',line['sku']) or line['sku'] in skus):
                raise ValueError('Invalid or duplicate receipt line')
            skus.add(line['sku'])
            received.setdefault(line['sku'],[]).append((money(line['quantity']),receipt['id']))
    findings=[]
    for line in invoice['lines']:
        sku=line['sku'];qty=money(line['quantity']);price=money(line['unitPrice'])
        po=ordered.get(sku);receipt=received.get(sku,[])
        values=[f'invoice quantity={qty}',f'invoice unitPrice={price}',invoice['currency']]
        evidence=[dict(sourceId=invoice['id'],pointer='/lines/'+str(invoice['lines'].index(line)))]
        if po is None:
            status='fail';reason='Invoice line absent from purchase order'
        elif not receipt:
            status='needs review';reason='Receipt missing; do not infer delivery'
        else:
            po_qty=money(po['quantity']);po_price=money(po['unitPrice']);receipt_qty=sum((item[0] for item in receipt),Decimal(0))
            values += [f'PO quantity={po_qty}',f'PO unitPrice={po_price}',f'received quantity={receipt_qty}']
            evidence += ([dict(sourceId=purchase_order['id'],pointer='/lines/'+str(purchase_order['lines'].index(po)))] +
                         [dict(sourceId=item[1],pointer='/lines') for item in receipt])
            passed=abs(qty-po_qty)<=tolerance and abs(qty-receipt_qty)<=tolerance and price==po_price
            status='pass' if passed else 'fail';reason='Exact decimal three-way match' if passed else 'Quantity or unit-price discrepancy'
        findings.append(finding(sku,status,reason,values,evidence))
    return findings


def invoice_from_sources(sources,document_id):
    """Closed invoice-lines-1 intake; incomplete OCR never yields a partial match."""
    pages=[source for source in sources if source.document_id==document_id]
    schema=[dict(id=name,labels=[label]) for name,label in (
        ('format','Invoice format'),('id','Invoice ID'),('purchaseOrderId','Purchase Order ID'),
        ('currency','Currency'),('lineCount','Line count'))]
    headers=required_fields(pages,schema)
    evidence=[item for header in headers for item in header['evidence']]
    if any(header['status']!='pass' for header in headers):
        return None,[finding('invoiceExtraction','needs review','Required invoice headers missing, unreadable or conflicting',(),evidence)],{}
    values={header['checkId']:header['values'][0] for header in headers}
    if values['format']!='invoice-lines-1' or not re.fullmatch(r'(?:[1-9][0-9]?|100)',values['lineCount']):
        return None,[finding('invoiceExtraction','unsupported','Use the explicit invoice-lines-1 template with a declared line count',(),evidence)],{}
    rows=[];row_evidence={};malformed=False
    for source in pages:
        for line in source.text.splitlines():
            if not line.lstrip().casefold().startswith('line:'):continue
            match=re.fullmatch(r'\s*Line:\s*([A-Za-z0-9_-]{1,64})\s*\|\s*(\d{1,12}(?:\.\d{1,4})?)\s*\|\s*(\d{1,12}(?:\.\d{1,4})?)\s*',line,re.I)
            if match is None:malformed=True;continue
            rows.append(dict(sku=match[1],quantity=match[2],unitPrice=match[3]))
            row_evidence.setdefault(match[1],[]).append(source.evidence(line))
    if malformed or len(rows)!=int(values['lineCount']) or len(row_evidence)!=len(rows):
        return None,[finding('invoiceExtraction','needs review','Invoice line rows are missing, duplicated or unreadable; no partial financial match released',(),evidence)],{}
    invoice={key:values[key] for key in ('id','purchaseOrderId','currency')};invoice['lines']=rows
    return invoice,[],{sku:evidence+locations for sku,locations in row_evidence.items()}
