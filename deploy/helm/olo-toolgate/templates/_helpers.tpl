{{/* Copyright 2026 OLO Labs; SPDX-License-Identifier: Apache-2.0 */}}
{{- define "olo-toolgate.name" -}}
{{- .Chart.Name | trunc 63 | trimSuffix "-" -}}
{{- end -}}
{{- define "olo-toolgate.gatewayName" -}}
{{- printf "%s-%s" .Release.Name .Chart.Name | trunc 46 | trimSuffix "-" -}}-gateway
{{- end -}}
{{- define "olo-toolgate.gatewaySelector" -}}
app.kubernetes.io/name: {{ include "olo-toolgate.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
app.kubernetes.io/component: gateway
{{- end -}}
{{- define "olo-toolgate.gatewayLabels" -}}
{{ include "olo-toolgate.gatewaySelector" . }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end -}}
