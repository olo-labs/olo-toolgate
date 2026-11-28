# Copyright 2026 OLO Labs; SPDX-License-Identifier: Apache-2.0
"""Opt-in real PDFium/Tesseract acceptance, with independently authored ground truth."""
import json
import os
from pathlib import Path
import tempfile
import unittest

from bot.business.fixtures import generate,generate_invoice
from bot.business.processor import process


@unittest.skipUnless(os.environ.get('OLO_BUSINESS_SCAN_ACCEPTANCE')=='1','Run in the processor image with OLO_BUSINESS_SCAN_ACCEPTANCE=1')
class BusinessScanAcceptanceTests(unittest.TestCase):
    def test_rotated_raster_pdf_preserves_actual_values_and_unreadable_email(self):
        with tempfile.TemporaryDirectory() as temporary:
            output=Path(temporary)/'ocr';generate(output)
            value=process('ocr',json.loads((output/'ocr-request.json').read_text()))
            truth=json.loads((output/'ocr-ground-truth.json').read_text())
            self.assertEqual(truth,{item['checkId']:item['values'] for item in value['report']['findings']})
            self.assertEqual('needs review',value['report']['findings'][-1]['status'])
            self.assertEqual(3,len(value['sources']))
            self.assertEqual({0,270},{source['rotation'] for source in value['sources']})
            self.assertEqual({'tesseract-eng'},{source['engine'] for source in value['sources']})
            for finding in value['report']['findings']:
                for citation in finding['evidence']:
                    source=next(s for s in value['sources'] if s['sourceId']==citation['sourceId'])
                    self.assertIn(citation['quote'],source['text'])
                    self.assertEqual(source['sha256'],citation['sha256'])

    def test_raster_invoice_reconciles_decimals_discrepancy_and_missing_receipt(self):
        with tempfile.TemporaryDirectory() as temporary:
            output=Path(temporary)/'invoice';generate_invoice(output)
            value=process('invoice',json.loads((output/'invoice-request.json').read_text()))
            truth=json.loads((output/'invoice-ground-truth.json').read_text())
            findings=value['report']['findings']
            self.assertEqual(truth['statuses'],{f['checkId']:f['status'] for f in findings})
            for finding in findings:
                sku=finding['checkId']
                self.assertIn('invoice quantity='+truth['invoiceQuantities'][sku],finding['values'])
                self.assertIn('invoice unitPrice='+truth['invoiceUnitPrices'][sku],finding['values'])
                citations=[e for e in finding['evidence'] if e['engine']=='tesseract-eng']
                self.assertTrue(citations)
                self.assertTrue(any(e['quote'].startswith('Line: '+sku) for e in citations))
                self.assertTrue(all(e['page']==1 and e['sha256']==value['sources'][0]['sha256'] for e in citations))
            self.assertEqual('tesseract-eng',value['sources'][0]['engine'])
