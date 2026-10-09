# Copyright 2026 OLO Labs
# SPDX-License-Identifier: Apache-2.0
"""Production Gateway image, real native effects and online revocation/outage proof."""
import argparse,json,shutil,sys
from pathlib import Path
sys.path.insert(0,str(Path(__file__).resolve().parents[1]))
from control.check import ROOT,run
from quickstart.check import build_images,smoke

def main():
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('--image',default='olo-toolgate-gateway:module05');p.add_argument('--helper-image',default='olo-toolgate-quickstart:enterprise-validation');p.add_argument('--no-build',action='store_true');args=p.parse_args()
    if not args.no_build:
        build_images(args.helper_image)
        run(['docker','build','-f','apps/gateway/Dockerfile','--build-arg','VERSION='+(ROOT/'VERSION').read_text().strip(),
            '--build-arg','REVISION='+run(['git','rev-parse','HEAD'],capture_output=True,text=True).stdout.strip(),'-t',args.image,'.'])
    smoke(args.helper_image,False,gateway_image=args.image)
    output=ROOT/'build/gateway';output.mkdir(parents=True,exist_ok=True)
    shutil.copyfile(ROOT/'build/quickstart/smoke.json',output/'container-smoke.json')
    print('Production Gateway/native effects/revocation/outage/custody gate passed')
if __name__=='__main__':main()
