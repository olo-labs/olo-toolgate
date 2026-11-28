# Copyright 2026 OLO Labs; SPDX-License-Identifier: Apache-2.0
"""Actual deterministic business findings, independent of model output."""
import base64
from copy import deepcopy
import unittest

from bot.business.documents import extract,intake
from bot.business.rules import required_fields,structural_netlist,reconcile,invoice_from_sources
from bot.business.processor import process


def text(document_id,content):
    return dict(id=document_id,mediaType='text/plain',data=base64.b64encode(content.encode()).decode())


class BusinessRulesTests(unittest.TestCase):
    def test_required_fields_keep_conflicting_dates_and_unreadable_fields(self):
        sources=intake([text('referral','Referring department: [missing]\nDischarge date: 2026-01-09'),
                        text('laboratory','Recorded discharge date: 2026-01-10')])
        findings=required_fields(sources,[dict(id='department',labels=['Referring department']),
                                          dict(id='date',labels=['Discharge date','Recorded discharge date'],format='date')])
        self.assertEqual(['needs review','fail'],[item['status'] for item in findings])
        self.assertEqual(['2026-01-09','2026-01-10'],findings[1]['values'])
        self.assertEqual('referral:p1',findings[0]['evidence'][0]['sourceId'])
        self.assertEqual(1,findings[0]['evidence'][0]['line'])
        for evidence in findings[1]['evidence']:
            source=next(s for s in sources if s.id==evidence['sourceId'])
            self.assertIn(evidence['quote'],source.text)

    def test_intake_rejects_unsupported_malformed_and_duplicate_documents(self):
        for doc in [dict(id='x',mediaType='application/zip',data='eA=='),
                    dict(id='../x',mediaType='text/plain',data='eA=='),
                    dict(id='x',mediaType='text/plain',data='!!!!'),
                    dict(id='x',mediaType='application/pdf',data='eA==')]:
            with self.subTest(document=doc),self.assertRaises(ValueError):extract(doc)
        with self.assertRaises(ValueError):intake([text('x','value'),text('x','other')])

    def test_narrow_structural_parser_cannot_turn_malformed_input_into_pass(self):
        checklist=dict(version='structural-verilog-1',rules=[
            dict(id='clock',kind='port',name='clk',direction='input'),
            dict(id='ready',kind='signal',name='ready'),dict(id='timing',kind='timing')])
        good='module chip(input clk, output done); wire active; assign done = active; endmodule'
        result=structural_netlist(intake([text('netlist',good)]),'netlist',checklist)
        self.assertEqual(['pass','fail','unsupported'],[f['status'] for f in result])
        for malformed in [good.replace('endmodule',''),good.replace('wire active','always @(clk) active'),
                          good.replace('input clk','input [7:0] clk'),good.replace('wire active','wire active; wire active')]:
            with self.subTest(netlist=malformed),self.assertRaises(ValueError):
                structural_netlist(intake([text('netlist',malformed)]),'netlist',checklist)

    def test_decimal_three_way_matching_separates_missing_receipts_and_differences(self):
        invoice=dict(id='invoice-1',purchaseOrderId='po-1',currency='USD',lines=[
            dict(sku='A',quantity='2',unitPrice='0.10'),dict(sku='B',quantity='5',unitPrice='20'),
            dict(sku='C',quantity='1',unitPrice='30')])
        po=deepcopy(invoice);po['id']='po-1';po['lines'][1]['quantity']='4'
        receipts=[dict(id='receipt-1',purchaseOrderId='po-1',lines=[dict(sku='A',quantity='2'),dict(sku='B',quantity='4')])]
        findings=reconcile(invoice,po,receipts)
        self.assertEqual(['pass','fail','needs review'],[f['status'] for f in findings])
        for bad in ['NaN','Infinity','-1','1e100','1.12345']:
            wrong=deepcopy(invoice);wrong['lines'][0]['unitPrice']=bad
            with self.subTest(price=bad),self.assertRaises(ValueError):reconcile(wrong,po,receipts)
        with self.assertRaises(ValueError):reconcile(invoice,po,receipts+receipts)
        wrong=deepcopy(po);wrong['currency']='EUR'
        with self.assertRaises(ValueError):reconcile(invoice,wrong,receipts)

    def test_invoice_source_rows_are_exactly_cited_and_match_independent_expectations(self):
        source='\n'.join(['Invoice format: invoice-lines-1','Invoice ID: INV-42',
            'Purchase Order ID: PO-42','Currency: USD','Line count: 3',
            'Line: A | 2 | 0.10','Line: B | 5 | 20.00','Line: C | 1 | 30.00'])
        po=dict(id='PO-42',purchaseOrderId='PO-42',currency='USD',lines=[
            dict(sku='A',quantity='2',unitPrice='0.10'),dict(sku='B',quantity='4',unitPrice='20.00'),
            dict(sku='C',quantity='1',unitPrice='30.00')])
        receipt=dict(id='GRN-42',purchaseOrderId='PO-42',lines=[dict(sku='A',quantity='2'),dict(sku='B',quantity='4')])
        result=process('invoice',dict(documents=[text('invoice-scan',source)],invoiceDocumentId='invoice-scan',
                                      purchaseOrder=po,receipts=[receipt]))
        findings=result['report']['findings']
        self.assertEqual({'A':'pass','B':'fail','C':'needs review'},{f['checkId']:f['status'] for f in findings})
        self.assertIn('invoice unitPrice=0.10',findings[0]['values'])
        self.assertIn('invoice quantity=5',findings[1]['values'])
        for item,expected_line in zip(findings,(6,7,8)):
            citations=[e for e in item['evidence'] if e['documentId']=='invoice-scan']
            row=next(e for e in citations if e['line']==expected_line)
            self.assertEqual(source.splitlines()[expected_line-1],row['quote'])
            self.assertEqual(result['sources'][0]['sha256'],row['sha256'])
            self.assertEqual('invoice-scan:p1',row['sourceId'])
            self.assertEqual('',row['pointer'])

    def test_missing_conflicting_or_malformed_invoice_rows_never_release_a_partial_match(self):
        good='\n'.join(['Invoice format: invoice-lines-1','Invoice ID: INV-42',
            'Purchase Order ID: PO-42','Currency: USD','Line count: 2',
            'Line: A | 2 | 0.10','Line: B | 5 | 20.00'])
        cases=[good.replace('Currency: USD',''),good+'\nInvoice ID: INV-43',
               good.replace('Line count: 2','Line count: 3'),good.replace('Line: B | 5 | 20.00','Line: B | [unreadable] | 20.00'),
               good.replace('Line: B','Line: A'),good.replace('Line: B | 5 | 20.00',''),
               good.replace('invoice-lines-1','unknown-template')]
        for source in cases:
            with self.subTest(source=source):
                invoice,findings,locations=invoice_from_sources(intake([text('scan',source)]),'scan')
                self.assertIsNone(invoice)
                self.assertEqual({},locations)
                self.assertEqual(1,len(findings))
                self.assertIn(findings[0]['status'],('needs review','unsupported'))
                self.assertEqual([],findings[0]['values'])

    def test_financial_record_bounds_reject_ambiguous_or_non_decimal_records(self):
        invoice=dict(id='I',purchaseOrderId='P',currency='USD',lines=[dict(sku='A',quantity='2',unitPrice='0.10')])
        po=deepcopy(invoice);po['id']='P'
        receipt=dict(id='R',purchaseOrderId='P',lines=[dict(sku='A',quantity='2')])
        invalid=[]
        for key,value in [('id','../I'),('lines',[]),('lines',[invoice['lines'][0]]*101),('currency','usd')]:
            bad=deepcopy(invoice);bad[key]=value;invalid.append((bad,po,[receipt]))
        bad=deepcopy(invoice);bad['lines'][0]['quantity']=2;invalid.append((bad,po,[receipt]))
        bad=deepcopy(invoice);bad['lines'][0]['discount']='0';invalid.append((bad,po,[receipt]))
        bad=deepcopy(receipt);bad['lines']*=2;invalid.append((invoice,po,[bad]))
        bad=deepcopy(receipt);bad['purchaseOrderId']='another';invalid.append((invoice,po,[bad]))
        for args in invalid:
            with self.subTest(args=args),self.assertRaises(ValueError):reconcile(*args)

    def test_quantity_tolerance_is_explicit_and_does_not_relax_unit_price(self):
        invoice=dict(id='I',purchaseOrderId='P',currency='USD',lines=[dict(sku='A',quantity='1.9999',unitPrice='0.10')])
        po=dict(id='P',purchaseOrderId='P',currency='USD',lines=[dict(sku='A',quantity='2',unitPrice='0.10')])
        receipt=dict(id='R',purchaseOrderId='P',lines=[dict(sku='A',quantity='2')])
        self.assertEqual('fail',reconcile(invoice,po,[receipt])[0]['status'])
        self.assertEqual('pass',reconcile(invoice,po,[receipt],'0.0001')[0]['status'])
        wrong=deepcopy(invoice);wrong['lines'][0]['unitPrice']='0.11'
        self.assertEqual('fail',reconcile(wrong,po,[receipt],'0.0001')[0]['status'])

    def test_domain_requests_reject_unknown_fields_and_invalid_rule_configuration(self):
        bad=[dict(documents=[],tenantId='forged'),dict(documents=[],fieldSchema=[dict(id='field',labels=['Name'],required='yes')]),
             dict(documents=[],requiredDocuments=[[]]),dict(documents=[],fieldSchema=[dict(id='field',labels=['Name'])]*2)]
        for request in bad:
            with self.subTest(request=request),self.assertRaises(ValueError):process('ocr',request)
        for request in [dict(invoice={},invoiceDocumentId='scan',documents=[]),dict(documents=[])]:
            with self.subTest(request=request),self.assertRaises(ValueError):process('invoice',request)
