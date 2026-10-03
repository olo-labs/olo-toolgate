# Upstream notices missing from Cargo archives

Some locked upstream crates omit their repository-root license files from the
published Cargo archive. These original notices come from the exact Git commit
in each archive's `.cargo_vcs_info.json`, with immutable source URLs recorded in
`SOURCE.txt`. They are bundled by `tools/client/package.py` without modification.
Crates that ship notices directly retain those files from the locked registry.

Update a notice only when changing the corresponding locked dependency version;
review the package license expression and upstream copyright at that commit.
Packaging rejects any reachable dependency without original notices. No runtime
network fetch or license waiver is used. The all-platform SBOM intentionally
retains reachable platform-specific dependencies for the six-target release.
