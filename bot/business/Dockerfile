# Copyright 2026 OLO Labs; SPDX-License-Identifier: Apache-2.0
FROM python:3.12-slim-bookworm
RUN apt-get update && apt-get install -y --no-install-recommends tesseract-ocr tesseract-ocr-eng fonts-dejavu-core \
    && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY bot/business/requirements.txt /app/requirements.txt
RUN pip install --no-cache-dir -r /app/requirements.txt
COPY bot/__init__.py /app/bot/__init__.py
COPY bot/business /app/bot/business
USER 65532:65532
EXPOSE 8443
ENTRYPOINT ["python", "-m", "bot.business.server"]
