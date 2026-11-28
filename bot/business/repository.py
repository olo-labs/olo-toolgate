# Copyright 2026 OLO Labs; SPDX-License-Identifier: Apache-2.0
"""Read-only intake repository. Linux directory descriptors prevent path/symlink escape."""
import hashlib
import json
import os
from pathlib import Path
import re

ID=re.compile(r'[A-Za-z0-9_-]{1,96}')
MAX_TOTAL=2*1024*1024
MEDIA={'application/pdf','image/png','image/jpeg','text/plain','text/markdown'}


def unique(items):
    result={}
    for key,value in items:
        if key in result:raise ValueError('Duplicate manifest key')
        result[key]=value
    return result


class Repository:
    def __init__(self,root):
        if os.name!='posix' or not hasattr(os,'O_NOFOLLOW'):
            raise ValueError('The intake repository requires its Linux processor image')
        self.root=os.open(root,os.O_RDONLY|os.O_DIRECTORY|os.O_NOFOLLOW)

    def close(self):
        os.close(self.root)

    def _open(self,intake_id):
        if not ID.fullmatch(intake_id):raise ValueError('Invalid intake identifier')
        return os.open(intake_id,os.O_RDONLY|os.O_DIRECTORY|os.O_NOFOLLOW,dir_fd=self.root)

    @staticmethod
    def _read(folder,name,maximum):
        if Path(name).name!=name or name in ('','.', '..'):
            raise ValueError('Invalid source filename')
        descriptor=os.open(name,os.O_RDONLY|os.O_NOFOLLOW|os.O_NONBLOCK,dir_fd=folder)
        with os.fdopen(descriptor,'rb') as stream:
            import stat
            if not stat.S_ISREG(os.fstat(stream.fileno()).st_mode):raise ValueError('Regular source file required')
            data=stream.read(maximum+1)
            if len(data)>maximum:raise ValueError('Source byte limit exceeded')
            return data

    def _manifest(self,folder):
        data=json.loads(self._read(folder,'manifest.json',16384),object_pairs_hook=unique,
            parse_constant=lambda _:(_ for _ in ()).throw(ValueError('Invalid JSON number')))
        if (not isinstance(data,dict) or set(data)!={'schemaVersion','useCase','documents','request'}
                or type(data['schemaVersion']) is not int or data['schemaVersion']!=1 or not isinstance(data['request'],dict)
                or 'documents' in data['request'] or not isinstance(data['documents'],list)
                or len(data['documents'])>16):raise ValueError('Invalid current intake manifest')
        ids=set();total=0
        for document in data['documents']:
            if (not isinstance(document,dict) or set(document)!={'id','mediaType','file','bytes','sha256'}
                    or not all(isinstance(document[key],str) for key in ('id','mediaType','file','sha256'))
                    or not ID.fullmatch(document['id'])
                    or document['id'] in ids or document['mediaType'] not in MEDIA
                    or Path(document['file']).name!=document['file'] or document['file'] in ('','.', '..')
                    or type(document['bytes']) is not int or not 1<=document['bytes']<=MAX_TOTAL
                    or not re.fullmatch('sha256:[a-f0-9]{64}',document['sha256'])):
                raise ValueError('Invalid intake document declaration')
            ids.add(document['id']);total+=document['bytes']
        if total>MAX_TOTAL:raise ValueError('Total intake byte limit exceeded')
        return data

    def manifest(self,intake_id):
        folder=self._open(intake_id)
        try:data=self._manifest(folder)
        finally:os.close(folder)
        return {**data,'documents':[{key:value for key,value in doc.items() if key!='file'} for doc in data['documents']]}

    def document(self,intake_id,document_id):
        if not ID.fullmatch(document_id):raise ValueError('Invalid document identifier')
        folder=self._open(intake_id)
        try:
            manifest=self._manifest(folder)
            document=next((doc for doc in manifest['documents'] if doc['id']==document_id),None)
            if document is None:raise FileNotFoundError('Document is outside this intake')
            data=self._read(folder,document['file'],MAX_TOTAL)
            if len(data)!=document['bytes'] or 'sha256:'+hashlib.sha256(data).hexdigest()!=document['sha256']:
                raise ValueError('Declared source content changed')
            return data,document['mediaType']
        finally:os.close(folder)
