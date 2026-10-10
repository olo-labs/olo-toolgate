# Copyright 2026 OLO Labs
# SPDX-License-Identifier: Apache-2.0
import json,glob,os,re,sys,collections
# usage: summarize.py <resultdir>
d=sys.argv[1]
rows=[]
for sd in sorted(glob.glob(os.path.join(d,'checks','server-*'))):
    name=re.sub(r'^server-','',os.path.basename(sd)); name=re.sub(r'-\d{4}-\d\d-\d\dT.*$','',name)
    try: cs=json.load(open(os.path.join(sd,'checks.json')))
    except Exception as e: rows.append((name,0,0,['<no checks.json>'])); continue
    p=sum(1 for c in cs if c.get('status')=='SUCCESS'); f=[c for c in cs if c.get('status')=='FAILURE']
    rows.append((name,p,len(f),[f"{c.get('id')}: {(c.get('errorMessage') or '')[:140]}" for c in f]))
for n,p,f,fl in rows:
    print(f"{'PASS' if f==0 else 'FAIL'} {n}: {p} passed, {f} failed")
    for x in fl: print("      - "+x)
