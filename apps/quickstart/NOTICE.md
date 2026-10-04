# Quickstart Python runtime notices

Runtime requirements are pinned in `requirements.txt`. Their distribution metadata
and full license files ship under
`/usr/share/licenses/olo-toolgate/quickstart-python/`.

Psycopg 3.3.6 is used unmodified under LGPL-3.0-only as a separately replaceable
Python package dynamically linking the distribution's libpq. Its full corresponding
source archive, verified against the pinned SHA-256, ships alongside its license.
Users may replace this library in their derived image. ToolGate source licensing
remains Apache-2.0. Native libpq notices remain under `/usr/share/doc/libpq5/`.

Redis-py 8.1.0 is MIT; typing-extensions 4.15.0 is PSF-2.0. The other packages retain
their upstream notices. Container SBOM and dependency/license scans include these
runtime dependencies; cache deployment does not cache authoritative security state.
