# Copyright 2026 OLO Labs; SPDX-License-Identifier: Apache-2.0
"""Bounded document decoding and source-linked text extraction.

Run this parser in the separately confined processor image, never in Control.
"""
import base64
import binascii
from dataclasses import dataclass
import hashlib
import io
import json
import re
import subprocess
import tempfile
import time
from pathlib import Path

MAX_DOCUMENT_BYTES = 2*1024*1024
MAX_PAGES = 10
MAX_PIXELS = 8_000_000
MAX_TEXT = 64_000
IDENTIFIER = re.compile(r'[A-Za-z0-9][A-Za-z0-9_.-]{0,95}')


@dataclass(frozen=True)
class Source:
    document_id: str
    page: int
    text: str
    sha256: str
    engine: str
    rotation: int = 0

    @property
    def id(self):
        return f'{self.document_id}:p{self.page}'

    def evidence(self, quote):
        if not quote or quote not in self.text:
            raise ValueError('Evidence must quote the source exactly')
        offset = self.text.index(quote)
        return dict(sourceId=self.id, documentId=self.document_id, page=self.page,
                    line=1+self.text[:offset].count('\n'), quote=quote, sha256=self.sha256,
                    engine=self.engine, rotation=self.rotation)


def _ocr(image,deadline):
    from PIL import ImageOps
    if image.width*image.height > MAX_PIXELS:
        raise ValueError('Image pixel limit exceeded')
    image = ImageOps.exif_transpose(image).convert('L')
    # Evaluate explicit right-angle orientations. Tesseract confidence is used
    # only to select orientation; it is not a calibrated field correctness score.
    candidates=[]
    with tempfile.TemporaryDirectory(prefix='olo-ocr-') as temporary:
        for rotation in (0, 90, 180, 270):
            path=Path(temporary)/f'page-{rotation}.png'
            image.rotate(rotation, expand=True).save(path)
            remaining=deadline-time.monotonic()
            if remaining<=0:
                raise ValueError('Document processing deadline exceeded')
            result=subprocess.run(['tesseract',str(path),'stdout','-l','eng','--psm','6','tsv'],
                                  capture_output=True,check=True,timeout=min(10,remaining))
            if len(result.stdout)>512_000:
                raise ValueError('OCR output limit exceeded')
            lines={}; scores=[]
            for row in result.stdout.decode('utf-8',errors='strict').splitlines()[1:]:
                cells=row.split('\t',11)
                if len(cells)!=12 or not cells[11].strip():
                    continue
                confidence=float(cells[10])
                if confidence<0:
                    continue
                key=tuple(cells[1:5]);lines.setdefault(key,[]).append(cells[11])
                scores.append(confidence)
            text='\n'.join(' '.join(words) for words in lines.values())
            score=sum(scores)/max(1,len(scores))
            candidates.append((score,text,rotation))
    _,text,rotation=max(candidates,key=lambda item:item[0])
    return text,rotation


def extract(document,deadline=None):
    deadline=deadline if deadline is not None else time.monotonic()+45
    if set(document)!={'id','mediaType','data'} or not IDENTIFIER.fullmatch(document['id']):
        raise ValueError('Invalid document envelope')
    if not isinstance(document['data'],str) or len(document['data'])>4*((MAX_DOCUMENT_BYTES+2)//3):
        raise ValueError('Document byte limit exceeded')
    try:
        raw=base64.b64decode(document['data'],validate=True)
    except (ValueError,binascii.Error):
        raise ValueError('Document data must be strict base64') from None
    if not raw or len(raw)>MAX_DOCUMENT_BYTES:
        raise ValueError('Document byte limit exceeded')
    digest='sha256:'+hashlib.sha256(raw).hexdigest()
    kind=document['mediaType'];pages=[]
    if kind in ('text/plain','text/markdown'):
        text=raw.decode('utf-8',errors='strict')
        if '\0' in text or len(text)>MAX_TEXT:
            raise ValueError('Invalid or oversized document text')
        return [Source(document['id'],1,text,digest,'utf-8')]
    if kind=='application/pdf':
        if not raw.startswith(b'%PDF-'):
            raise ValueError('PDF signature mismatch')
        import pypdfium2 as pdfium
        with pdfium.PdfDocument(raw) as pdf:
            if not 1<=len(pdf)<=MAX_PAGES:
                raise ValueError('PDF page limit exceeded')
            for page in pdf:
                textpage=page.get_textpage()
                try:
                    text=textpage.get_text_range()
                finally:
                    textpage.close()
                if text.strip():
                    pages.append((text,'pdfium-text',0))
                else:
                    width,height=page.get_size()
                    if width*height*4>MAX_PIXELS:
                        raise ValueError('Rendered page pixel limit exceeded')
                    bitmap=page.render(scale=2)
                    try:
                        text,rotation=_ocr(bitmap.to_pil(),deadline)
                    finally:
                        bitmap.close()
                    pages.append((text,'tesseract-eng',rotation))
                page.close()
    elif kind in ('image/png','image/jpeg'):
        from PIL import Image
        if (kind=='image/png' and not raw.startswith(b'\x89PNG\r\n\x1a\n')
                or kind=='image/jpeg' and not raw.startswith(b'\xff\xd8')):
            raise ValueError('Image signature mismatch')
        Image.MAX_IMAGE_PIXELS=MAX_PIXELS
        with Image.open(io.BytesIO(raw)) as image:
            if getattr(image,'n_frames',1)!=1:
                raise ValueError('Multi-frame images are unsupported')
            text,rotation=_ocr(image,deadline)
            pages.append((text,'tesseract-eng',rotation))
    else:
        raise ValueError('Unsupported document media type')
    if sum(len(text) for text,_,_ in pages)>MAX_TEXT:
        raise ValueError('Extracted text limit exceeded')
    return [Source(document['id'],index,text,digest,engine,rotation)
            for index,(text,engine,rotation) in enumerate(pages,1)]


def intake(documents):
    if not isinstance(documents,list) or not 0<=len(documents)<=16:
        raise ValueError('Supply at most sixteen documents')
    if len({document['id'] for document in documents})!=len(documents):
        raise ValueError('Duplicate document IDs')
    deadline=time.monotonic()+45
    sources=[source for document in documents for source in extract(document,deadline)]
    if len(sources)>32 or sum(len(source.text) for source in sources)>MAX_TEXT:
        raise ValueError('Intake text/page limit exceeded')
    return sources


def fields(sources, labels):
    """Extract explicitly configured labelled fields without interpreting instructions."""
    found={name:[] for name in labels}
    for source in sources:
        for line in source.text.splitlines():
            match=re.fullmatch(r'\s*([^:\n]{1,80}):\s*(.*?)\s*',line)
            if not match:
                continue
            for name,aliases in labels.items():
                if match[1].casefold() in {alias.casefold() for alias in aliases}:
                    value=match[2]
                    if value.casefold() in ('','[missing]','[unreadable]','missing','unreadable'):
                        value=''
                    found[name].append(dict(value=value,evidence=source.evidence(line)))
    return found
