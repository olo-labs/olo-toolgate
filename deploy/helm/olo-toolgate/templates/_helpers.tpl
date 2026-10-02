{{/* Copyright 2026 OLO Labs; SPDX-License-Identifier: Apache-2.0 */}}
{{- define "olo-toolgate.name" -}}
{{- .Chart.Name | trunc 63 | trimSuffix "-" -}}
{{- end -}}
