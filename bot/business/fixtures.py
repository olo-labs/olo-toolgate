# Copyright 2026 OLO Labs; SPDX-License-Identifier: Apache-2.0
"""Generate synthetic scan fixtures in an explicitly selected output directory."""
import argparse
import base64
import json
from pathlib import Path


def generate(output):
    from PIL import Image, ImageDraw, ImageFont, PngImagePlugin
    output.mkdir(parents=True,exist_ok=True)
    if any(output.iterdir()):
        raise ValueError('Choose a fresh fixture output directory; existing samples are preserved')
    font=ImageFont.truetype('/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf',24)
    pages=[]
    texts=[['SYNTHETIC SUPPLIER REGISTRATION','Organization: Sample Supplies Ltd','Registration number: SYN-0042'],
           ['SYNTHETIC SUPPLIER REGISTRATION','Address: 42 Example Road','City: Fictional City'],
           ['SYNTHETIC SUPPLIER REGISTRATION','Contact email:']]
    notice=PngImagePlugin.PngInfo()
    notice.add_text('License','Copyright 2026 OLO Labs; SPDX-License-Identifier: Apache-2.0')
    for index,lines in enumerate(texts,1):
        image=Image.new('1',(700,900),1);draw=ImageDraw.Draw(image)
        for row,line in enumerate(lines):draw.text((35,50+row*70),line,font=font,fill=0)
        draw.text((35,800),'Fictional sample - no real supplier data',font=font,fill=0)
        if index==3:draw.rectangle((225,115,560,150),fill=0)
        if index==2:image=image.rotate(90,expand=True)
        image.save(output/f'supplier-page-{index}.png',pnginfo=notice)
        pages.append(image)
    pdf=output/'supplier-registration.pdf'
    pages[0].save(pdf,save_all=True,append_images=pages[1:],resolution=100,
                  title='Synthetic supplier registration',author='OLO Labs',
                  subject='Apache-2.0 synthetic OCR evaluation fixture')
    request=dict(documents=[dict(id='supplier',mediaType='application/pdf',data=base64.b64encode(pdf.read_bytes()).decode())],
        fieldSchema=[dict(id='organizationName',labels=['Organization']),
                     dict(id='registrationNumber',labels=['Registration number']),
                     dict(id='address',labels=['Address']),dict(id='city',labels=['City']),
                     dict(id='contactEmail',labels=['Contact email'],format='email')])
    (output/'ocr-request.json').write_text(json.dumps(request,indent=2)+'\n',encoding='utf-8')
    (output/'ocr-ground-truth.json').write_text(json.dumps(dict(
        organizationName=['Sample Supplies Ltd'],registrationNumber=['SYN-0042'],
        address=['42 Example Road'],city=['Fictional City'],contactEmail=[]),indent=2)+'\n',encoding='utf-8')
    # PDF contains raster pages only: text extraction cannot substitute for OCR.
    import pypdfium2 as pdfium
    with pdfium.PdfDocument(pdf) as document:
        for index,page in enumerate(document,1):
            text=page.get_textpage()
            try:
                if text.get_text_range().strip():raise ValueError('Scan fixture unexpectedly contains a text layer')
            finally:
                text.close()
            bitmap=page.render(scale=1.3)
            try:bitmap.to_pil().save(output/f'pdf-preview-{index}.png')
            finally:bitmap.close();page.close()
    return dict(pdfBytes=pdf.stat().st_size,requestBytes=(output/'ocr-request.json').stat().st_size)


def generate_invoice(output):
    """A raster-only explicit invoice template with independent matching truth."""
    from PIL import Image, ImageDraw, ImageFont, PngImagePlugin
    output.mkdir(parents=True,exist_ok=True)
    if any(output.iterdir()):
        raise ValueError('Choose a fresh invoice fixture directory; existing samples are preserved')
    font=ImageFont.truetype('/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf',34)
    image=Image.new('1',(1100,1450),1);draw=ImageDraw.Draw(image)
    lines=['SYNTHETIC INVOICE - FICTIONAL DATA','Invoice format: invoice-lines-1',
           'Invoice ID: INV-0042','Purchase Order ID: PO-0042','Currency: USD','Line count: 3',
           'Line: A | 2 | 0.10','Line: B | 5 | 20.00','Line: C | 1 | 30.00']
    for row,line in enumerate(lines):draw.text((45,65+row*90),line,font=font,fill=0)
    draw.text((45,1330),'No payment or real supplier.',font=font,fill=0)
    notice=PngImagePlugin.PngInfo()
    notice.add_text('License','Copyright 2026 OLO Labs; SPDX-License-Identifier: Apache-2.0')
    image.save(output/'invoice.png',pnginfo=notice)
    pdf=output/'invoice.pdf'
    image.save(pdf,resolution=100,title='Synthetic invoice-lines-1 invoice',author='OLO Labs',
               subject='Apache-2.0 synthetic invoice OCR evaluation fixture')
    request=dict(documents=[dict(id='invoice-scan',mediaType='application/pdf',data=base64.b64encode(pdf.read_bytes()).decode())],
        invoiceDocumentId='invoice-scan',purchaseOrder=dict(id='PO-0042',purchaseOrderId='PO-0042',currency='USD',lines=[
            dict(sku='A',quantity='2',unitPrice='0.10'),dict(sku='B',quantity='4',unitPrice='20.00'),
            dict(sku='C',quantity='1',unitPrice='30.00')]),
        receipts=[dict(id='GRN-0042',purchaseOrderId='PO-0042',lines=[dict(sku='A',quantity='2'),dict(sku='B',quantity='4')])])
    (output/'invoice-request.json').write_text(json.dumps(request,indent=2)+'\n',encoding='utf-8')
    # This is authored independently from parser output: B has an invoice/PO
    # quantity discrepancy and C has no receipt. A must retain decimal 0.10.
    truth=dict(invoiceId='INV-0042',purchaseOrderId='PO-0042',currency='USD',
               statuses=dict(A='pass',B='fail',C='needs review'),invoiceQuantities=dict(A='2',B='5',C='1'),
               invoiceUnitPrices=dict(A='0.10',B='20.00',C='30.00'))
    (output/'invoice-ground-truth.json').write_text(json.dumps(truth,indent=2)+'\n',encoding='utf-8')
    import pypdfium2 as pdfium
    with pdfium.PdfDocument(pdf) as document:
        page=document[0];text=page.get_textpage()
        try:
            if text.get_text_range().strip():raise ValueError('Invoice scan unexpectedly contains a text layer')
        finally:text.close()
        bitmap=page.render(scale=1.3)
        try:bitmap.to_pil().save(output/'invoice-preview.png')
        finally:bitmap.close();page.close()
    return dict(pdfBytes=pdf.stat().st_size,requestBytes=(output/'invoice-request.json').stat().st_size)


if __name__=='__main__':
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--output',type=Path,required=True)
    parser.add_argument('--case',choices=('ocr','invoice'),default='ocr')
    args=parser.parse_args()
    print(json.dumps((generate_invoice if args.case=='invoice' else generate)(args.output)))
