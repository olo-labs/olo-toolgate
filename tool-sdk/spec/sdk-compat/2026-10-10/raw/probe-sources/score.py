# Copyright 2026 OLO Labs
# SPDX-License-Identifier: Apache-2.0
import sys,re,json,glob,os
# usage: score.py <list-req.txt> <resultdir>
lines=open(sys.argv[1]).read().split('\n')
req=[];mode=None
for l in lines:
    if l.startswith('Server scenarios'): mode='s'; continue
    if l.startswith('Client scenarios') or l.startswith('Run and reported'): mode=None
    m=re.match(r'\s+- (\S+)$',l)
    if mode=='s' and m: req.append(m.group(1))
d=sys.argv[2]; res={}
for sd in glob.glob(os.path.join(d,'checks','server-*')):
    n=re.sub(r'-\d{4}-\d\d-\d\dT.*$','',os.path.basename(sd)[7:])
    p=os.path.join(sd,'checks.json')
    if not os.path.exists(p): continue
    cs=json.load(open(p))
    res[n]=(sum(c['status']=='SUCCESS' for c in cs), sum(c['status']=='FAILURE' for c in cs))
ok=[r for r in req if r in res and res[r][1]==0]; bad=[r for r in req if r not in ok]
print(f"{d}: required server scenarios {len(ok)}/{len(req)} passing; checks passed={sum(res[r][0] for r in req if r in res)} failed={sum(res[r][1] for r in req if r in res)}; failing: {bad}")
