# Copyright 2026 OLO Labs; SPDX-License-Identifier: Apache-2.0
"""Actual HTTP handler boundaries; TLS and Control integration qualify separately."""
from concurrent.futures import ThreadPoolExecutor
import http.client
import json
import threading
import unittest
import hashlib
from unittest.mock import patch

from bot.business.server import listener


class RepositorySpy:
    def __init__(self):self.calls=[]
    def manifest(self,name):
        self.calls.append(('manifest',name))
        return dict(schemaVersion=1,useCase='ocr',documents=[],request={})
    def document(self,name,document):
        self.calls.append(('document',name,document))
        return b'Organization: Fictional','text/plain'


class BusinessProcessorHttpTests(unittest.TestCase):
    def setUp(self):
        self.repository=RepositorySpy()
        self.server=listener(('127.0.0.1',0),b'x'*32,'tenant-one',self.repository)
        self.thread=threading.Thread(target=self.server.serve_forever,daemon=True);self.thread.start()
        self.addCleanup(self.stop)

    def stop(self):
        self.server.shutdown();self.server.server_close();self.thread.join(timeout=3)

    def request(self,method,path='/v1/process',raw=None,headers=None,api_key='x'*32):
        connection=http.client.HTTPConnection(*self.server.server_address,timeout=3)
        try:
            connection.putrequest(method,path)
            supplied=[('Content-Type','application/json')]
            if api_key is not None:supplied.append(('X-Api-Key',api_key))
            supplied.extend(headers or [])
            if raw is not None and not any(name.lower()=='content-length' for name,_ in supplied):
                supplied.append(('Content-Length',str(len(raw))))
            for name,value in supplied:connection.putheader(name,value)
            connection.endheaders(raw)
            response=connection.getresponse();body=response.read()
            return response.status,body,dict(response.headers)
        finally:connection.close()

    @staticmethod
    def envelope(request=None,tenant='tenant-one'):
        return json.dumps(dict(tenantId=tenant,useCase='ocr',request=json.dumps(request or dict(documents=[])))).encode()

    def test_valid_request_produces_findings_and_no_store_cache_headers(self):
        status,raw,headers=self.request('POST',raw=self.envelope())
        self.assertEqual(200,status)
        self.assertEqual('needs review',json.loads(raw)['report']['findings'][0]['status'])
        self.assertEqual('no-store',headers['Cache-Control'])
        self.assertEqual(len(raw),int(headers['Content-Length']))

    def test_exact_intake_request_processes_repository_and_returns_manifest_digest(self):
        envelope=json.dumps(dict(tenantId='tenant-one',useCase='ocr',intakeId='sample')).encode()
        manifest=self.repository.manifest('sample');self.repository.calls.clear()
        status,raw,_=self.request('POST','/v1/process-intake',raw=envelope)
        self.assertEqual(200,status);value=json.loads(raw)
        self.assertEqual('sample',value['intakeId'])
        self.assertEqual('sha256:'+hashlib.sha256(json.dumps(manifest,separators=(',',':')).encode()).hexdigest(),value['manifestSha256'])
        self.assertEqual([('manifest','sample')],self.repository.calls)
        self.assertEqual('needs review',value['processing']['report']['findings'][0]['status'])

    def test_changed_source_bytes_fail_intake_without_partial_report(self):
        source=b'Original bytes'
        manifest=dict(schemaVersion=1,useCase='ocr',request={},documents=[dict(id='source',mediaType='text/plain',bytes=len(source),sha256='sha256:'+hashlib.sha256(source).hexdigest())])
        envelope=json.dumps(dict(tenantId='tenant-one',useCase='ocr',intakeId='sample')).encode()
        with patch.object(self.repository,'manifest',return_value=manifest):
            status,raw,_=self.request('POST','/v1/process-intake',raw=envelope)
        self.assertEqual(400,status);self.assertNotIn('processing',json.loads(raw))

    def test_inspection_verifies_sources_without_running_ocr_or_returning_source_text(self):
        envelope=json.dumps(dict(tenantId='tenant-one',useCase='ocr',intakeId='sample')).encode()
        with patch('bot.business.server.process') as process:
            status,raw,_=self.request('POST','/v1/inspect-intake',raw=envelope)
        self.assertEqual(200,status)
        self.assertEqual({'intakeId','manifestSha256'},set(json.loads(raw)))
        process.assert_not_called()

    def test_inspection_stays_available_while_bounded_ocr_slots_are_busy(self):
        entered=threading.Barrier(3);release=threading.Event()
        def blocked(*args):
            entered.wait(timeout=3);release.wait(timeout=3)
            return dict(report={},sources=[])
        with patch('bot.business.server.process',side_effect=blocked),ThreadPoolExecutor(max_workers=2) as executor:
            pending=[executor.submit(self.request,'POST',raw=self.envelope()) for _ in range(2)]
            try:
                entered.wait(timeout=3)
                envelope=json.dumps(dict(tenantId='tenant-one',useCase='ocr',intakeId='sample')).encode()
                self.assertEqual(200,self.request('POST','/v1/inspect-intake',raw=envelope)[0])
            finally:release.set()
            self.assertTrue(all(item.result()[0]==200 for item in pending))

    def test_wrong_tenant_and_traversal_never_read_an_intake(self):
        for tenant,identifier in [('another-tenant','sample'),('tenant-one','../sample')]:
            raw=json.dumps(dict(tenantId=tenant,useCase='ocr',intakeId=identifier)).encode()
            status,_,_=self.request('POST','/v1/process-intake',raw=raw)
            self.assertIn(status,(400,403))
        self.assertEqual([],self.repository.calls)

    def test_cross_tenant_or_duplicate_api_key_is_refused_before_processing(self):
        with patch('bot.business.server.process') as process:
            for raw,headers in [(self.envelope(tenant='tenant-two'),[]),
                                (self.envelope(),[('X-Api-Key','attacker-key')])]:
                with self.subTest(headers=headers):
                    status,body,_=self.request('POST',raw=raw,headers=headers)
                    self.assertEqual(403,status)
                    self.assertEqual(dict(code='FORBIDDEN'),json.loads(body))
            for api_key in (None,'wrong-api-key'):
                status,body,_=self.request('POST',raw=self.envelope(),api_key=api_key)
                self.assertEqual(403,status)
                self.assertEqual(dict(code='FORBIDDEN'),json.loads(body))
            process.assert_not_called()

    def test_duplicate_json_keys_nonfinite_numbers_and_unknown_envelopes_are_refused(self):
        requests=[b'{"tenantId":"tenant-one","tenantId":"tenant-two","useCase":"ocr","request":"{}"}',
                  json.dumps(dict(tenantId='tenant-one',useCase='ocr',request='{"documents":[],"number":NaN}')).encode(),
                  b'{"tenantId":"tenant-one","useCase":"ocr","request":NaN}',
                  self.envelope(dict(documents=[],extra='unapproved')),b'[]',b'null']
        with patch('bot.business.server.process',wraps=__import__('bot.business.processor',fromlist=['process']).process):
            for raw in requests:
                with self.subTest(raw=raw):
                    status,body,_=self.request('POST',raw=raw)
                    self.assertIn(status,(400,403))
                    self.assertIn(json.loads(body)['code'],('VALIDATION','FORBIDDEN'))

    def test_ambiguous_framing_and_large_body_are_refused(self):
        for headers,expected in [([('Transfer-Encoding','chunked')],400),
                                 ([('Content-Length','2'),('Content-Length','2')],400),
                                 ([('Content-Length',str(3*1024*1024+1))],413)]:
            with self.subTest(headers=headers):
                status,_,_=self.request('POST',raw=b'{}',headers=headers)
                self.assertEqual(expected,status)

    def test_read_routes_require_one_verified_tenant_and_do_not_list_paths(self):
        for path,headers,expected in [('/v1/intakes/sample',[],403),
                ('/v1/intakes/sample',[('X-OLO-Tenant','tenant-two')],403),
                ('/v1/intakes/sample',[('X-OLO-Tenant','tenant-one'),('X-OLO-Tenant','tenant-one')],403),
                ('/v1/intakes/../secret',[('X-OLO-Tenant','tenant-one')],404),
                ('/v1/intakes/sample?list=true',[('X-OLO-Tenant','tenant-one')],404),
                ('/v1/intakes',[('X-OLO-Tenant','tenant-one')],404)]:
            with self.subTest(path=path,headers=headers):
                status,_,_=self.request('GET',path,headers=headers)
                self.assertEqual(expected,status)
        self.assertEqual([],self.repository.calls)
        status,_,_=self.request('GET','/v1/intakes/sample',headers=[('X-OLO-Tenant','tenant-one')])
        self.assertEqual(200,status)
        status,body,headers=self.request('GET','/v1/intakes/sample/documents/document',headers=[('X-OLO-Tenant','tenant-one')])
        self.assertEqual(200,status)
        self.assertEqual(b'Organization: Fictional',body)
        self.assertEqual('text/plain',headers['Content-Type'])

    def test_result_limit_and_exceptions_never_release_partial_or_private_data(self):
        with patch('bot.business.server.process',return_value={'value':'x'*1_048_576}):
            status,body,_=self.request('POST',raw=self.envelope())
            self.assertEqual(413,status);self.assertEqual(dict(code='RESULT_LIMIT'),json.loads(body))
        with patch('bot.business.server.process',side_effect=RuntimeError('secret private source')):
            status,body,_=self.request('POST',raw=self.envelope())
            self.assertEqual(500,status);self.assertNotIn(b'secret',body)

    def test_concurrency_limit_refuses_excess_work_and_releases_capacity(self):
        entered=threading.Barrier(3);release=threading.Event()
        def processing(*args):
            entered.wait(timeout=5);release.wait(timeout=5)
            return dict(report={},sources=[])
        with patch('bot.business.server.process',side_effect=processing):
            with ThreadPoolExecutor(max_workers=2) as executor:
                first=[executor.submit(self.request,'POST',raw=self.envelope()) for _ in range(2)]
                try:
                    entered.wait(timeout=5)
                    status,body,_=self.request('POST',raw=self.envelope())
                    self.assertEqual(429,status);self.assertEqual(dict(code='BUSY'),json.loads(body))
                finally:release.set()
                self.assertEqual([200,200],[future.result()[0] for future in first])
        self.assertEqual(200,self.request('POST',raw=self.envelope())[0])
