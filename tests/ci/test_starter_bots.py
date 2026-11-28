# Copyright 2026 OLO Labs; SPDX-License-Identifier: Apache-2.0
"""Starter source/sample and release integrity; no live provider qualification."""
from copy import deepcopy
import io
import json
from pathlib import Path
import tempfile
import unittest
import subprocess
from unittest.mock import patch
import zipfile

from bot.build import build
from bot.bundle import digest, inventory, MAX_BUNDLE, verify
from bot.starters import build_kit, configurations, evaluate, verify_kit
from bot import quickstart
from tools.release.profiles import collect
from tests.ci.starter_fixture import business_package,sample_result

ROOT = Path(__file__).resolve().parents[2]
CASES = {'ocr', 'medical', 'vlsic', 'loan', 'invoice', 'support', 'itsm', 'onboarding'}


class StarterTests(unittest.TestCase):
    def setUp(self):
        package_patch=patch('bot.build.source_package',side_effect=business_package)
        package_patch.start();self.addCleanup(package_patch.stop)
    def test_selected_starters_prepare_the_allowlisted_model_once_per_setup(self):
        with tempfile.TemporaryDirectory() as temporary:
            prepared=set()
            with patch.object(quickstart.subprocess,'run') as run:
                for folder in configurations().values():
                    row=build(folder,Path(temporary),'1.0.0-test','a'*40,'test',{},None)[0]
                    quickstart.prepare_local_models((Path(temporary)/row['filename']).read_bytes(),prepared)
                self.assertEqual(1,run.call_count)
                self.assertEqual(['prepare-model','--model','qwen3:4b'],run.call_args.args[0][-3:])
                self.assertEqual({'qwen3:4b'},prepared)

    def test_model_preparation_failure_is_not_marked_prepared_or_retried(self):
        with tempfile.TemporaryDirectory() as temporary:
            row=build(configurations()['starter-loan'],Path(temporary),'1.0.0-test','a'*40,'test',{},None)[0]
            prepared=set()
            with patch.object(quickstart.subprocess,'run',side_effect=subprocess.CalledProcessError(1,'fixture')) as run:
                with self.assertRaises(subprocess.CalledProcessError):
                    quickstart.prepare_local_models((Path(temporary)/row['filename']).read_bytes(),prepared)
                self.assertEqual(set(),prepared)
                self.assertEqual(1,run.call_count)

    def test_all_eight_sources_build_verify_and_preserve_distinct_identity(self):
        self.assertEqual({'starter-'+name for name in CASES}, set(configurations()))
        with tempfile.TemporaryDirectory() as temporary:
            rows=[]
            for name, folder in sorted(configurations().items()):
                with self.subTest(name=name):
                    rows += build(folder, Path(temporary), '1.0.0-starter.test', 'a'*40, 'test', {}, None)
                    raw=(Path(temporary)/rows[-1]['filename']).read_bytes()
                    self.assertEqual(name, verify(raw)['workflowId'])
                    files=inventory(raw,MAX_BUNDLE,256*1024*1024)
                    workflow=json.loads(files['workflow.json'])['workflows']['workflows'][0]
                    self.assertEqual(['process','analyze','review'],[node['id'] for node in workflow['graph']['config']['nodes']])
                    self.assertEqual(['olo.business'],[item['packageId'] for item in rows[-1]['packages']])
            kit=build_kit(rows,'1.0.0-starter.test','a'*40)
            self.assertEqual(kit,build_kit(rows,'1.0.0-starter.test','a'*40))
            self.assertEqual(rows,verify_kit(kit,rows,'1.0.0-starter.test','a'*40)['profiles'])
            for bad_rows,version,commit in [(rows[:-1],'1.0.0-starter.test','a'*40),(rows,'1.0.1','a'*40),(rows,'1.0.0-starter.test','b'*40)]:
                with self.assertRaises(ValueError):verify_kit(kit,bad_rows,version,commit)

    def test_every_authored_sample_and_missing_source_expectation_is_evaluable(self):
        for name, folder in configurations().items():
            for exception in (False,True):
                with self.subTest(name=name,exception=exception):
                    result=sample_result(folder,exception)
                    value=evaluate(folder,folder/('exception-input.txt' if exception else 'sample-input.txt'),result,
                                   folder/('expected-exception-report.json' if exception else 'expected-report.json'))
                    self.assertGreater(value['checks'],0)

    def test_lost_conflicts_fabricated_quotes_duplicates_and_cross_input_results_are_rejected(self):
        folder=configurations()['starter-loan'];original=sample_result(folder)
        mutations=[]
        bad=deepcopy(original);bad['report']['findings'][1]['values'].pop();mutations.append(bad)
        bad=deepcopy(original);bad['report']['findings'][0]['evidence'][0]['quote']='not in source';mutations.append(bad)
        bad=deepcopy(original);bad['report']['findings'].append(bad['report']['findings'][0]);mutations.append(bad)
        bad=deepcopy(original);bad['submittedInput']='{}';mutations.append(bad)
        bad=deepcopy(original);bad['report']['findings'][1]['status']='pass';mutations.append(bad)
        for result in mutations:
            with self.assertRaises(ValueError):evaluate(folder,folder/'sample-input.txt',result,folder/'expected-report.json')

    def test_release_collector_requires_exact_source_kit_before_writing_assets(self):
        folder=configurations()['starter-loan']
        with tempfile.TemporaryDirectory() as temporary:
            root=Path(temporary);source=root/'source';source.mkdir()
            rows=build(folder,source,'1.0.0-starter.test','a'*40,'test',{},None)
            index=dict(format='olo-profiles',version='1.0.0-starter.test',sourceCommit='a'*40,profiles=rows)
            path=source/'olo-profiles-1.0.0-starter.test.json'
            with patch('tools.release.profiles.expected_profiles',return_value={('starter-loan','starter-loan')}):
                path.write_text(json.dumps(index))
                with self.assertRaisesRegex(ValueError,'starter source kit'):collect(source,root/'missing')
                self.assertFalse((root/'missing').exists())
                kit=build_kit(rows,index['version'],index['sourceCommit']);kit_path=source/'olo-starters-1.0.0-starter.test.zip'
                kit_path.write_bytes(kit)
                index['starterKit']=dict(filename=kit_path.name,sha256=digest(kit),bytes=len(kit))
                path.write_text(json.dumps(index))
                self.assertIn(kit_path.name,collect(source,root/'accepted'))
                kit_path.write_bytes(kit+b'changed')
                with self.assertRaisesRegex(ValueError,'checksum'):collect(source,root/'tampered')
                self.assertFalse((root/'tampered').exists())

    def test_kit_missing_source_is_rejected_even_with_updated_outer_hash(self):
        with tempfile.TemporaryDirectory() as temporary:
            rows=build(configurations()['starter-loan'],Path(temporary),'1.0.0-test','a'*40,'test',{},None)
            raw=build_kit(rows,'1.0.0-test','a'*40)
            with zipfile.ZipFile(io.BytesIO(raw)) as archive:files={name:archive.read(name) for name in archive.namelist()}
            del files['starter-loan/sample-input.txt']
            manifest=json.loads(files['manifest.json']);manifest['files'].pop('starter-loan/sample-input.txt')
            files['manifest.json']=json.dumps(manifest).encode()
            output=io.BytesIO()
            with zipfile.ZipFile(output,'w') as archive:
                for name,data in files.items():archive.writestr(name,data)
            with self.assertRaisesRegex(ValueError,'Incomplete'):verify_kit(output.getvalue(),rows,'1.0.0-test','a'*40)
