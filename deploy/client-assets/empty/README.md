# Client release assets

This directory contains no binaries. The console reports downloads unavailable.
Build a release bundle with `tools/client/manifest.py`, then build Control with
`--build-arg CLIENT_ASSETS_DIR=deploy/client-assets/release`
`--build-arg CLIENT_DOWNLOADS_DIRECTORY=/opt/toolgate/client-downloads`.
The release directory is ignored by Git; package binaries are release artifacts.
