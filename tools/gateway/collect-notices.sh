#!/bin/sh
# Copyright 2026 OLO Labs
# SPDX-License-Identifier: Apache-2.0
# Preserve notices for compiled Rust/std/musl dependencies in the shipped image.
set -eu
output="$1"
mkdir -p "$output/cargo" "$output/rust"
cp /src/Cargo.lock "$output/Cargo.lock"
find /usr/local/cargo/registry/src -type f \( -iname 'license*' -o -iname 'copying*' -o -iname 'notice*' -o -iname 'copyright*' -o -iname 'readme*' \) |
while IFS= read -r notice; do
    relative="${notice#/usr/local/cargo/registry/src/}"
    mkdir -p "$output/cargo/$(dirname "$relative")"
    cp "$notice" "$output/cargo/$relative"
done
for docs in /usr/local/rustup/toolchains/*/share/doc/rust; do
    cp "$docs/COPYRIGHT-library.html" "$output/rust/"
    cp "$docs/COPYRIGHT.html" "$output/rust/"
    cp -R "$docs/licenses" "$output/rust/"
done
test -s "$output/rust/COPYRIGHT-library.html"
grep -qi musl "$output/rust/COPYRIGHT.html"
