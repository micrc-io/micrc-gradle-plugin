{{/*
Expand the name of the chart.
*/}}
{{- define "${name}.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Database secret name
*/}}
{{- define "${name}.database.secretName" -}}
{{- printf "%s-%s" (include "${name}.name" .) "database" }}
{{- end }}

{{/*
Cache secret name
*/}}
{{- define "${name}.cache.secretName" -}}
{{- printf "%s-%s" (include "${name}.name" .) "cache" }}
{{- end }}

{{/*
Broker secret name
*/}}
{{- define "${name}.broker.secretName" -}}
{{- printf "%s-%s" (include "${name}.name" .) "broker" }}
{{- end }}

{{/*
Create a default fully qualified app name.
We truncate at 63 chars because some Kubernetes name fields are limited to this (by the DNS naming spec).
If release name contains chart name it will be used as a full name.
*/}}
{{- define "${name}.fullname" -}}
{{- if .Values.fullnameOverride }}
{{- .Values.fullnameOverride | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- \$name := default .Chart.Name .Values.nameOverride }}
{{- if contains \$name .Release.Name }}
{{- .Release.Name | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- printf "%s-%s" .Release.Name \$name | trunc 63 | trimSuffix "-" }}
{{- end }}
{{- end }}
{{- end }}

{{/*
Create chart name and version as used by the chart label.
*/}}
{{- define "${name}.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Common labels
*/}}
{{- define "${name}.labels" -}}
helm.sh/chart: {{ include "${name}.chart" . }}
{{ include "${name}.selectorLabels" . }}
{{- if .Chart.AppVersion }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end }}

{{/*
Selector labels
*/}}
{{- define "${name}.selectorLabels" -}}
app.kubernetes.io/name: {{ include "${name}.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}

{{/*
Create the name of the service account to use
*/}}
{{- define "${name}.serviceAccountName" -}}
{{- if .Values.serviceAccount.create }}
{{- default (include "${name}.fullname" .) .Values.serviceAccount.name }}
{{- else }}
{{- default "default" .Values.serviceAccount.name }}
{{- end }}
{{- end }}

{{/*
role name
*/}}
{{- define "${name}.roleName" -}}
{{- printf "%s-%s" (include "${name}.name" .) "namespace-reader" }}
{{- end }}

{{/*
role binding name
*/}}
{{- define "${name}.roleBindingName" -}}
{{- printf "%s-%s" (include "${name}.name" .) "namespace-reader-binding" }}
{{- end }}
