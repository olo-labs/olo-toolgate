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
    sample=json.loads(result.stdout.strip().splitlines()[-1])
    sample.update(host=platform.platform(),hostCpu=platform.processor(),toolchain='Rust 1.94.1',execution='Docker rust:1.94-bookworm' if DOCKER else 'native',networkIncluded=False,collectorIoIncluded=False)
    folder=ROOT/'build/gateway'; folder.mkdir(parents=True,exist_ok=True)
    (folder/'benchmark.json').write_text(json.dumps(sample,indent=2)+'\n',encoding='utf-8',newline='\n')
    print(json.dumps(sample,indent=2))


if __name__=='__main__': main()
