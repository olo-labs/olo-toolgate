# Copyright 2026 OLO Labs; SPDX-License-Identifier: Apache-2.0
"""Authenticated TLS document-processor listener. No orchestration or model calls."""
import argparse
import hmac
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
import json
from pathlib import Path
import ssl
import threading
import re
import base64
import hashlib

from bot.business.processor import process


def unique(items):
    result={}
    for key,value in items:
        if key in result:
            raise ValueError('Duplicate JSON key')
        result[key]=value
    return result


def listener(address,api_key,tenant,repository=None):
    semaphore=threading.BoundedSemaphore(2)
    inspection_semaphore=threading.BoundedSemaphore(2)
    class Handler(BaseHTTPRequestHandler):
        server_version='OLOBusinessProcessor'

        def setup(self):
            super().setup()
            self.connection.settimeout(15)

        def authenticated(self,*,read=False):
            if len(self.headers.get_all('X-Api-Key',[]))!=1:
                return False
            if not hmac.compare_digest(self.headers.get('X-Api-Key','').encode(),api_key):
                return False
            return not read or (len(self.headers.get_all('X-OLO-Tenant',[]))==1 and self.headers['X-OLO-Tenant']==tenant)

        def log_message(self,*args):
            pass  # Inputs, request paths and credentials never enter HTTP logs.

        def reply(self,status,value):
            raw=json.dumps(value,separators=(',',':'),allow_nan=False).encode()
            if len(raw)>1_048_576:
                status=413;raw=b'{"code":"RESULT_LIMIT"}'
            self.send_response(status)
            self.send_header('Content-Type','application/json')
            self.send_header('Cache-Control','no-store')
            self.send_header('Content-Length',str(len(raw)))
            self.end_headers();self.wfile.write(raw)

        def do_GET(self):
            if not self.authenticated(read=True):
                self.reply(403,dict(code='FORBIDDEN'));return
            if repository is None:
                self.reply(404,dict(code='SOURCE_UNAVAILABLE'));return
            match=re.fullmatch(r'/v1/intakes/([A-Za-z0-9_-]{1,96})(?:/documents/([A-Za-z0-9_-]{1,96}))?',self.path)
            if match is None:
                self.reply(404,dict(code='SOURCE_UNAVAILABLE'));return
            try:
                if match[2] is None:
                    self.reply(200,repository.manifest(match[1]));return
                raw,media_type=repository.document(match[1],match[2])
                self.send_response(200);self.send_header('Content-Type',media_type)
                self.send_header('Cache-Control','no-store');self.send_header('Content-Length',str(len(raw)))
                self.end_headers();self.wfile.write(raw)
            except FileNotFoundError:
                self.reply(404,dict(code='SOURCE_UNAVAILABLE'))
            except (ValueError,TypeError,KeyError,OSError):
                self.reply(409,dict(code='SOURCE_CHANGED'))

        def do_POST(self):
            self.connection.settimeout(15)
            if not self.authenticated():
                self.reply(403,dict(code='FORBIDDEN'));return
            if self.path not in ('/v1/process','/v1/process-intake','/v1/inspect-intake') or self.headers.get('Content-Type','').split(';')[0]!='application/json':
                self.reply(400,dict(code='VALIDATION'));return
            if (self.headers.get('Transfer-Encoding') or len(self.headers.get_all('Content-Length',[]))!=1
                    or not self.headers.get('Content-Length','').isdigit()):
                self.reply(400,dict(code='VALIDATION'));return
            length=int(self.headers['Content-Length'])
            if not 1<=length<=3*1024*1024:
                self.reply(413,dict(code='VALIDATION'));return
            gate=inspection_semaphore if self.path=='/v1/inspect-intake' else semaphore
            if not gate.acquire(blocking=False):
                self.reply(429,dict(code='BUSY'));return
            try:
                raw=self.rfile.read(length)
                if len(raw)!=length:
                    raise ValueError('Truncated input')
                envelope=json.loads(raw,object_pairs_hook=unique,parse_constant=lambda _:(_ for _ in ()).throw(ValueError('Invalid JSON number')))
                fields={'tenantId','useCase','intakeId'} if self.path in ('/v1/process-intake','/v1/inspect-intake') else {'tenantId','useCase','request'}
                if set(envelope)!=fields or envelope['tenantId']!=tenant:
                    self.reply(403,dict(code='FORBIDDEN'));return
                if self.path in ('/v1/process-intake','/v1/inspect-intake'):
                    if repository is None:raise ValueError('Intake repository required')
                    identifier=envelope['intakeId']
                    if not isinstance(identifier,str) or not re.fullmatch('[A-Za-z0-9_-]{1,96}',identifier):
                        raise ValueError('Invalid intake identifier')
                    manifest=repository.manifest(identifier)
                    if manifest['useCase']!=envelope['useCase']:raise ValueError('Intake use case differs')
                    request=manifest['request'];documents=[]
                    for document in manifest['documents']:
                        raw,media=repository.document(identifier,document['id'])
                        if (len(raw)!=document['bytes'] or media!=document['mediaType']
                                or 'sha256:'+hashlib.sha256(raw).hexdigest()!=document['sha256']):
                            raise ValueError('Source changed from observed manifest')
                        documents.append(dict(id=document['id'],mediaType=media,data=base64.b64encode(raw).decode('ascii')))
                    manifest_bytes=json.dumps(manifest,separators=(',',':'),allow_nan=False).encode()
                    if self.path=='/v1/inspect-intake':
                        self.reply(200,dict(intakeId=identifier,manifestSha256='sha256:'+hashlib.sha256(manifest_bytes).hexdigest()))
                        return
                    result=process(envelope['useCase'],{**request,'documents':documents})
                    self.reply(200,dict(processing=result,intakeId=identifier,
                        manifestSha256='sha256:'+hashlib.sha256(manifest_bytes).hexdigest()))
                    return
                request=json.loads(envelope['request'],object_pairs_hook=unique,
                    parse_constant=lambda _:(_ for _ in ()).throw(ValueError('Invalid JSON number')))
                result=process(envelope['useCase'],request)
                self.reply(200,result)
            except (ValueError,KeyError,TypeError,UnicodeError,OverflowError):
                self.reply(400,dict(code='VALIDATION'))
            except Exception:
                self.reply(500,dict(code='PROCESSING_FAILED'))
            finally:
                gate.release()
    return ThreadingHTTPServer(address,Handler)


def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--certificate',required=True,type=Path)
    parser.add_argument('--key',required=True,type=Path)
    parser.add_argument('--api-key-file',required=True,type=Path)
    parser.add_argument('--tenant',required=True)
    parser.add_argument('--port',type=int,default=8443)
    parser.add_argument('--intakes',type=Path,help='Read-only Linux intake directory; no upload or filesystem-write endpoint')
    args=parser.parse_args()
    key=args.api_key_file.read_bytes().strip()
    if not 32<=len(key)<=512:
        raise ValueError('Use a private generated service credential')
    from bot.business.repository import Repository
    repository=Repository(args.intakes) if args.intakes else None
    server=listener(('0.0.0.0',args.port),key,args.tenant,repository)
    context=ssl.SSLContext(ssl.PROTOCOL_TLS_SERVER);context.minimum_version=ssl.TLSVersion.TLSv1_2
    context.load_cert_chain(args.certificate,args.key)
    server.socket=context.wrap_socket(server.socket,server_side=True)
    server.serve_forever()


if __name__=='__main__':
    main()
