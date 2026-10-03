# Copyright 2026 OLO Labs
# SPDX-License-Identifier: Apache-2.0
$inputDocument = [Console]::In.ReadToEnd() | ConvertFrom-Json
@{protocolVersion=1;requestId=$inputDocument.requestId;output=@{text=$inputDocument.arguments.text}} | ConvertTo-Json -Compress -Depth 16
