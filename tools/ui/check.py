# Copyright 2026 OLO Labs
# SPDX-License-Identifier: Apache-2.0
"""Accessible console against a fresh group-only installation."""
import argparse,sys
from pathlib import Path
sys.path.insert(0,str(Path(__file__).resolve().parents[1]))
from quickstart.check import smoke

def main():
 parser=argparse.ArgumentParser(description=__doc__);parser.add_argument('--no-build',action='store_true');parser.add_argument('--image',default='olo-toolgate-quickstart:enterprise-validation');args=parser.parse_args();smoke(args.image,True)
if __name__=='__main__':main()
