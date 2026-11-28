# Copyright 2026 OLO Labs; SPDX-License-Identifier: Apache-2.0
"""Package editable starters and evaluate saved synthetic results; never execute bots."""
import argparse
import io
import json
from pathlib import Path
import sys
import zipfile

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT))
from bot.bundle import digest, encoded, inventory


def configurations():
    return {p.name: p for p in (ROOT/'bot/configurations').iterdir() if (p/'starter.json').is_file()}


def source_names(metadata):
    return metadata['files'] + ['bot.json', 'referenced-config.json', 'package-build.json', 'trigger-connections.json']


def build_kit(rows, version, commit):
    selected = [row for row in rows if row['configuration'] in configurations()]
    if not selected:
        return None
    payload = {name: (ROOT/name).read_bytes() for name in ('LICENSE', 'NOTICE.md')}
    payload['README.md'] = (ROOT/'bot/starters/README.md').read_bytes()
    for row in selected:
        folder = configurations()[row['configuration']]
        metadata = json.loads((folder/'starter.json').read_text(encoding='utf-8'))
        for name in source_names(metadata):
            path = folder/name
            if path.is_symlink() or path.name != name or not path.resolve().is_relative_to(folder.resolve()):
                raise ValueError('Unsafe starter source')
            payload[folder.name+'/'+name] = path.read_bytes()
    manifest = dict(format='olo-starter-sources', version=version, sourceCommit=commit,
                    qualification='not-qualified', profiles=selected,
                    files={name: digest(data) for name, data in sorted(payload.items())})
    payload['manifest.json'] = encoded(manifest)
    output = io.BytesIO()
    with zipfile.ZipFile(output, 'w', zipfile.ZIP_DEFLATED) as archive:
        for name, data in sorted(payload.items()):
            info = zipfile.ZipInfo(name, (1980, 1, 1, 0, 0, 0))
            info.compress_type = zipfile.ZIP_DEFLATED
            info.external_attr = 0o100644 << 16
            archive.writestr(info, data)
    return output.getvalue()


def verify_kit(data, rows, version, commit):
    files = inventory(data, 16*1024*1024, 32*1024*1024)
    manifest = json.loads(files.pop('manifest.json'))
    expected = [row for row in rows if row['configuration'] in configurations()]
    if (manifest.get('format') != 'olo-starter-sources' or manifest.get('version') != version
            or manifest.get('sourceCommit') != commit or manifest.get('profiles') != expected
            or manifest.get('qualification') != 'not-qualified'
            or manifest.get('files') != {name: digest(value) for name, value in files.items()}):
        raise ValueError('Starter source evidence mismatch')
    names = {'LICENSE', 'NOTICE.md', 'README.md'}
    for row in expected:
        folder = configurations()[row['configuration']]
        metadata = json.loads(files[folder.name+'/starter.json'])
        if metadata != json.loads((folder/'starter.json').read_text(encoding='utf-8')):
            raise ValueError('Starter metadata differs from publication source')
        names.update(folder.name+'/'+name for name in source_names(metadata))
    if set(files) != names:
        raise ValueError('Incomplete starter source kit')
    return manifest


def evaluate(folder, input_path, result, expected_path):
    """Check authored expectations and quotes, never infer success from a trace."""
    from jsonschema import Draft202012Validator
    profile = json.loads((folder/'bot.json').read_text(encoding='utf-8'))
    Draft202012Validator(profile['workflows']['workflows'][0]['resultSchema']).validate(result)
    submitted = input_path.read_text(encoding='utf-8').strip()
    if result['submittedInput'] != submitted:
        raise ValueError('Result is for another submitted input')
    prefix='exception' if input_path.name.startswith('exception-') else 'sample'
    intake=json.loads((folder/(prefix+'-intake.json')).read_text(encoding='utf-8'))
    documents={document['id']:document for document in intake['documents']}
    sources={source['sourceId']:source for source in result['sources']}
    if len(sources)!=len(result['sources']):
        raise ValueError('Duplicate source IDs')
    for source in sources.values():
        document=documents.get(source['documentId'])
        if document is None or source['sha256']!=document['sha256'] or source['sourceId']!=source['documentId']+':p'+str(source['page']):
            raise ValueError('Source does not match the pinned intake')
    expected = json.loads(expected_path.read_text(encoding='utf-8'))
    findings = result['report']['findings']
    actual = {finding['checkId']: finding for finding in findings}
    wanted = {finding['checkId']: finding for finding in expected['findings']}
    if len(actual) != len(findings) or set(actual) != set(wanted):
        raise ValueError('Missing, duplicate or unexpected finding')
    for check_id, finding in actual.items():
        target = wanted[check_id]
        if finding['status'] != target['status'] or set(finding['values']) != set(target['values']):
            raise ValueError('Finding differs from ground truth: '+check_id)
        if {e['documentId'] or e['sourceId'] for e in finding['evidence']} != {e['documentId'] or e['sourceId'] for e in target['evidence']}:
            raise ValueError('Evidence coverage differs: '+check_id)
        for item in finding['evidence']:
            if item['pointer']:
                records=[intake['request'].get(key,{}) for key in ('invoice','purchaseOrder')]+intake['request'].get('receipts',[])
                record=next((record for record in records if record.get('id')==item['sourceId']),None)
                if record is None or not item['pointer'].startswith('/lines') or item['quote']:
                    raise ValueError('Unknown structured evidence: '+check_id)
            else:
                source=sources.get(item['sourceId'])
                if source is None or not item['quote'] or item['quote'] not in source['text'] or item['sha256']!=source['sha256']:
                    raise ValueError('Invented evidence: '+check_id)
                if item['line']!=1+source['text'][:source['text'].index(item['quote'])].count('\n'):
                    raise ValueError('Incorrect source line: '+check_id)
    for commentary in (result['analysis'],result['review']):
        for citation in commentary['citations']:
            source=sources.get(citation['sourceId'])
            if source is None or citation['quote'] not in source['text']:
                raise ValueError('Invented commentary citation')
    return dict(checks=len(actual), evidenceReferences=sum(len(f['evidence']) for f in findings),
                scope='saved synthetic result; not live runtime or semantic answer qualification')


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--configuration', required=True, choices=sorted(configurations()))
    parser.add_argument('--result', required=True, type=Path, help='Saved result object, not the run envelope')
    parser.add_argument('--exception', action='store_true')
    args = parser.parse_args()
    folder = configurations()[args.configuration]
    input_name = 'exception-input.txt' if args.exception else 'sample-input.txt'
    expected_name = 'expected-exception-report.json' if args.exception else 'expected-report.json'
    print(json.dumps(evaluate(folder, folder/input_name,
        json.loads(args.result.read_text(encoding='utf-8')), folder/expected_name)))


if __name__ == '__main__':
    main()
