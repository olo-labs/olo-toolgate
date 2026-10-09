# Copyright 2026 OLO Labs
# SPDX-License-Identifier: Apache-2.0
"""Isolated PostgreSQL and SQLite enterprise conformance, with machine-readable evidence."""
import argparse,json,os,sys,xml.etree.ElementTree as ET
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2];sys.path.insert(0,str(ROOT/'tools'))
from control.check import database,run

def tests():
 with database() as db:
  env=dict(os.environ,CONTROL_TEST_URL=db['CONTROL_TEST_URL'],CONTROL_TEST_PASSWORD=db['CONTROL_TEST_PASSWORD'])
  wrapper=ROOT/('gradlew.bat' if os.name=='nt' else 'gradlew')
  run([str(wrapper),'--no-daemon',':contracts-java:test',':control-plane:test',':control-plane:enterpriseTest','--console=plain'],env=env)
 suites=[]
 for folder in ('packages/contracts/java/build/test-results','apps/control-plane/build/test-results'):
  for p in sorted((ROOT/folder).rglob('TEST-*.xml')):
   r=ET.parse(p).getroot();suites.append(dict(name=r.attrib['name'],tests=int(r.attrib['tests']),failures=int(r.attrib['failures']),errors=int(r.attrib['errors']),cases=[c.attrib['name'] for c in r.findall('testcase')]))
 evidence=ROOT/'build/enterprise';evidence.mkdir(parents=True,exist_ok=True);(evidence/'conformance.json').write_text(json.dumps(dict(suites=suites,total=sum(s['tests'] for s in suites),failures=sum(s['failures']+s['errors'] for s in suites)),indent=2)+'\n',encoding='utf-8')
 print('PostgreSQL, SQLite and shared enterprise contract gates passed')
def main():
 argparse.ArgumentParser(description=__doc__).parse_args();tests()
if __name__=='__main__':main()
