# Configuration import folder

Mounted read-only at `/config/import`. Put an exported `server-settings.json` here to
seed server settings, such as device auto-approval, on first start. Set
`TOOLGATE_CONFIG_IMPORT_OVERWRITE=true` to apply it on every start instead. See
[configuration import and export](../../../../docs/control-plane/import-export.md).
