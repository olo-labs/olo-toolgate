# Copyright 2026 OLO Labs
# SPDX-License-Identifier: Apache-2.0
"""Record the measured core baseline and its execution context."""
import json
import platform
import sys
from pathlib import Path

ROOT=Path(__file__).resolve().parents[2]
sys.path.insert(0,str(ROOT/'tools'))
from check import DOCKER, run


def main():
    result=run(['cargo','bench','-p','olo-toolgate-gateway','--bench','authorization','--locked'],capture=True)
    folder=ROOT/'build/gateway'; folder.mkdir(parents=True,exist_ok=True)
    for line in result.stdout.strip().splitlines():
        if not line.startswith('{'):continue
        sample=json.loads(line)
        sample.update(host=platform.platform(),hostCpu=platform.processor(),toolchain='Rust 1.94.1',execution='Docker rust:1.94-bookworm' if DOCKER else 'native',networkIncluded=False,collectorIoIncluded=False)
        if sample['benchmark'] in ('signed-policy-evaluation', 'signed-ask-policy-evaluation'):
            sample['monotonicClock']='fixed for reproducible full-scan samples; production Instant sampling cost excluded'
        name={'authorization-core':'benchmark.json','signed-policy-evaluation':'bundle-benchmark.json',
              'signed-ask-policy-evaluation':'approval-benchmark.json'}[sample['benchmark']]
        (folder/name).write_text(json.dumps(sample,indent=2)+'\n',encoding='utf-8',newline='\n')
        print(json.dumps(sample,indent=2))


if __name__=='__main__': main()
