# Contributor-authored release notes

Add one YAML fragment per contribution under `.changes/v<VERSION>/`. For the
first beta, the reserved directory is `.changes/v0.1.0-beta.1/`. This convention
does not change `VERSION` or allocate a release number.

Use `pr-<number>-<short-kebab-slug>.yaml` (preferred) or
`issue-<number>-<short-kebab-slug>.yaml`, for example
`pr-123-gateway-timeout.yaml`. Use a real stable GitHub ID, not an author name,
sequence number, or placeholder. Each PR/issue ID can appear only once per version;
combine related changes in its `details`. Different PR and issue IDs are separate
namespaces. Both `.yaml` and `.yml` are accepted; use `.yaml` for new files.

Copy [Rahul Nigam's example](examples/pr-123-release-note-fragments.yaml), replace
its illustrative PR number and text, and put it in the agreed release directory.
`examples/` is excluded from release compilation. `.gitkeep` reserves an empty
directory; validation accepts it, but compilation rejects empty releases.

## Fields

The canonical [JSON Schema](schema.json) validates the parsed YAML object.

| Field | Required | Meaning |
|---|---|---|
| `author` | Yes | Contributor display name or GitHub handle, e.g. `Rahul Nigam` |
| `category` | Yes | `added`, `changed`, `deprecated`, `removed`, `fixed`, `security`, `docs`, or `internal` |
| `summary` | Yes | Nonblank, single-line user-facing description, up to 200 characters |
| `details` | Yes | Nonblank Markdown; explain behavior, impact, and migration steps when relevant |
| `breaking` | Yes | YAML boolean `true` or `false`; `true` visibly marks the entry **BREAKING** |
| `references` | Yes | Nonempty list of unique quoted `"#123"` references or HTTPS URLs |
| `component` | No | Lowercase kebab-case component such as `gateway`, `client`, or `release` |

Unknown fields, duplicate YAML keys, unsafe YAML tags, duplicate contribution IDs,
invalid filenames, nested directories and symlinked fragments are rejected.
Quote issue references: an unquoted `#123` is a YAML comment. Multiline `details`
uses `|`. A breaking entry must explain migration steps in `details`; reviewers
check their accuracy. Markdown is contributor-authored content and should be reviewed.

## Validate and preview

From the repository root, with Python 3.12:

```sh
python -m pip install -r tools/requirements.txt
python tools/release/notes.py validate
python tools/release/notes.py validate --version v0.1.0-beta.1
python tools/release/notes.py compile --version v0.1.0-beta.1 --output build/release-notes.md
python -m unittest discover -s tests/contracts -p test_release_fragments.py -v
```

`compile` also accepts `0.1.0-beta.1`; omitting `--output` prints Markdown to
stdout. It reads only the requested version and fails for missing/empty folders
or invalid fragments before writing output. Sections follow the category order
listed above; entries sort by filename using Python string order (lexical, not
numeric). Output is UTF-8 with LF newlines and contains no date or machine state.
Repeated compilation of the same files produces identical bytes. Input fragments
are retained for audit and reruns.

The **Release-note fragments** workflow validates relevant pull requests and can
be manually dispatched with a version to download a `release-note-preview`
artifact. It never creates a release. The normal Python foundation gate also
validates every release folder and runs the tests.

## Product release integration

Use the existing [release operator guide](../docs/development/how-to-release.md).
Preparation allocates the product version first. Agree on its target fragment
directory before preparing: an allocated `1.0.0-beta.42` reads exactly
`.changes/v1.0.0-beta.42/`, never the first-beta folder or another version.
If a failed/abandoned preparation consumes a build number, copy the intended
fragments to the newly allocated version in the source branch before preparing
again; keep historical folders. Do not change immutable prepared branches.

When the exact directory exists, `tools/release/prepare.py stamp` validates and
compiles it into a marked section at the start of `CHANGELOG.md` in the prepared
source tree. Repeating preparation replaces that version's section without
duplicating it. The existing `tools/contracts/version.py` and binding generator
then produce and distribute `RELEASE-NOTES.md`; the current bundle/publisher
already consumes that file. No alternate release publisher is introduced.
Existing changelog content is preserved below the generated section.

An exact directory opts the version into fragments: empty or invalid contents
stop preparation. With no exact directory, legacy changelog-based notes remain
available for older source commits and releases. Compilation is always explicit
and never falls back to another version. Development stamping remains unchanged.
Do not hand-edit generated `RELEASE-NOTES.md` files.
