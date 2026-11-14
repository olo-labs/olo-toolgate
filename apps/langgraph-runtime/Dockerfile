# Copyright 2026 OLO Labs
# SPDX-License-Identifier: Apache-2.0
ARG PYTHON_IMAGE=python:3.12-slim@sha256:5e2bdb1b0d542d5039cffb8181c65930093949194a65c365dfb1b0fe13a4fa5b
FROM ${PYTHON_IMAGE} AS dependencies
WORKDIR /app
COPY requirements.lock ./
RUN python -m venv /opt/toolgate-runtime \
    && /opt/toolgate-runtime/bin/python -m pip install --no-cache-dir -r requirements.lock \
    && /opt/toolgate-runtime/bin/python -m pip check

FROM ${PYTHON_IMAGE}
WORKDIR /app
COPY --from=dependencies /opt/toolgate-runtime /opt/toolgate-runtime
COPY src/ ./src/
ENV PYTHONPATH=/app/src PYTHONUNBUFFERED=1 PYTHONDONTWRITEBYTECODE=1
USER 65532:65532
ENTRYPOINT ["/opt/toolgate-runtime/bin/python", "-m", "toolgate_runtime.trigger_listener"]
