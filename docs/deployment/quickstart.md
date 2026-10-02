# Quickstart Deployment

## Purpose

Single container, `/data` volume, embedded DB/cache/vault, safe built-in tools, client downloads. Explicitly non-HA.

Module 03 supplies the console artifact embedded in Control at `/console/`, using
the versioned API on the same origin. Future Module 11 packaging reuses it and
adds local identity bootstrap; there is no default password or development-token
provider in the production artifact. Quickstart is still unimplemented.
