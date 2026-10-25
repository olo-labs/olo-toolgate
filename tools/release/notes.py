# Copyright 2026 OLO Labs
# SPDX-License-Identifier: Apache-2.0
"""Validate contributor fragments and compile one version without publishing anything."""
import argparse
import json
from pathlib import Path
import re
import sys

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT))
from tools.contracts.version import SEMVER

CATEGORIES = {
    'added': 'Added', 'changed': 'Changed', 'deprecated': 'Deprecated',
    'removed': 'Removed', 'fixed': 'Fixed', 'security': 'Security',
    'docs': 'Documentation', 'internal': 'Internal',
}
FILENAME = re.compile(r'(?:pr|issue)-([1-9][0-9]*)-[a-z0-9]+(?:-[a-z0-9]+)*\.ya?ml')


def version_name(value):
    """Accept the product version or its v-prefixed tag, never a path."""
    version = value.removeprefix('v')
    if not SEMVER.fullmatch(version):
        raise ValueError('Version must be SemVer core/prerelease without build metadata')
    return 'v'+version


def load_fragments(version, root=ROOT, allow_empty=False):
    # Keep YAML/schema dependencies lazy: the early metadata gate also handles
    # old source trees with no fragments using only the Python standard library.
    import yaml
    from jsonschema import Draft202012Validator
    from jsonschema.exceptions import ValidationError

    class UniqueLoader(yaml.SafeLoader):
        def construct_mapping(self, node, deep=False):
            keys = set()
            for key, _ in node.value:
                if not isinstance(key, yaml.ScalarNode) or key.value in keys:
                    raise ValueError('Duplicate or non-scalar YAML mapping key')
                keys.add(key.value)
            return super().construct_mapping(node, deep=deep)

    name = version_name(version)
    folder = root/'.changes'/name
    if folder.is_symlink() or not folder.is_dir():
        raise ValueError('Missing regular fragment directory: '+str(folder))
    schema = json.loads((root/'.changes/schema.json').read_text(encoding='utf-8'))
    Draft202012Validator.check_schema(schema)
    validator = Draft202012Validator(schema)
    fragments = []
    identifiers = set()
    for path in sorted(folder.iterdir()):
        if path.name == '.gitkeep' and path.is_file() and not path.is_symlink():
            continue
        match = FILENAME.fullmatch(path.name)
        if not match or path.is_symlink() or not path.is_file():
            raise ValueError(str(path)+': expected (pr|issue)-<ID>-<slug>.yaml')
        identifier = path.name.split('-', 1)[0]+'-'+match[1]
        if identifier in identifiers:
            raise ValueError(str(path)+': duplicate contribution ID '+identifier)
        identifiers.add(identifier)
        try:
            fragment = yaml.load(path.read_text(encoding='utf-8'), Loader=UniqueLoader)
            validator.validate(fragment)
            if not fragment['summary'].strip():
                raise ValueError('Summary must contain non-whitespace text')
        except ValidationError as error:
            raise ValueError(str(path)+': '+error.message) from error
        except (yaml.YAMLError, ValueError, RecursionError) as error:
            raise ValueError(str(path)+': '+str(error)) from error
        fragments.append((path.name, fragment))
    if not fragments and not allow_empty:
        raise ValueError('No release-note fragments in '+str(folder))
    return fragments


def validate_all(root=ROOT):
    """Validate every version, including pending versions different from VERSION."""
    for path in sorted((root/'.changes').iterdir()):
        if path.name in ('README.md', 'schema.json', 'examples'):
            continue
        if path.name != version_name(path.name):
            raise ValueError('Fragment directories must have a v-prefixed version: '+path.name)
        load_fragments(path.name, root, allow_empty=True)


def compile_notes(version, root=ROOT):
    name = version_name(version)
    fragments = load_fragments(name, root)
    lines = [f'## {name}', '']
    # Fixed category order, then filename order; no dates, locale or Git queries.
    for category, title in CATEGORIES.items():
        entries = [(filename, item) for filename, item in fragments if item['category'] == category]
        if not entries:
            continue
        lines += ['### '+title, '']
        for filename, item in entries:
            label = ('**BREAKING** ' if item['breaking'] else '')
            label += ('`'+item['component']+'`: ' if 'component' in item else '')
            lines += ['- '+label+item['summary'].strip(), '']
            lines += ['  '+line if line else '' for line in item['details'].strip().splitlines()]
            lines += ['', '  Author: '+item['author'].strip()+'. References: '+', '.join(item['references'])+'.',
                      '  Fragment: `'+filename+'`.', '']
    return '\n'.join(lines)


def update_changelog(version, root=ROOT):
    """Opt in by exact version directory; keep the canonical document generator."""
    name = version_name(version)
    folder = root/'.changes'/name
    if not folder.exists() and not folder.is_symlink():
        return  # Older source trees and versions retain the existing release notes.
    content = compile_notes(name, root)
    begin, end = '<!-- release-fragments:'+name+' -->', '<!-- /release-fragments:'+name+' -->'
    section = begin+'\n'+content+end+'\n\n'
    path = root/'CHANGELOG.md'
    previous = path.read_text(encoding='utf-8')
    if begin in previous or end in previous:
        if previous.count(begin) != 1 or previous.count(end) != 1 or previous.index(end) < previous.index(begin):
            raise ValueError('Malformed generated changelog section for '+name)
        previous = re.sub(re.escape(begin)+r'.*?'+re.escape(end)+r'\n*',
                          lambda _: section, previous, count=1, flags=re.S)
    else:
        previous = section+previous
    path.write_text(previous, encoding='utf-8', newline='\n')


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('operation', choices=('validate', 'compile'))
    parser.add_argument('--version', help='Product version or v-prefixed tag')
    parser.add_argument('--output', type=Path, help='Compile to a UTF-8/LF Markdown file (default: stdout)')
    args = parser.parse_args()
    if args.operation == 'compile' and not args.version:
        parser.error('compile requires --version')
    if args.operation == 'validate' and args.output:
        parser.error('--output is only supported by compile')
    try:
        if args.operation == 'validate':
            load_fragments(args.version) if args.version else validate_all()
            print('Release-note fragments verified')
        else:
            content = compile_notes(args.version)
            if args.output:
                args.output.parent.mkdir(parents=True, exist_ok=True)
                args.output.write_text(content, encoding='utf-8', newline='\n')
            else:
                sys.stdout.write(content)
    except (ValueError, OSError) as error:
        parser.exit(1, str(error)+'\n')


if __name__ == '__main__':
    main()
