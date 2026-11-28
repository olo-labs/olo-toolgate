# Copyright 2026 OLO Labs; SPDX-License-Identifier: Apache-2.0
FROM eclipse-temurin@sha256:a8bff88f760e87e0c8844f8461f807c203fd5e8f78c951c843024647e965eaf5
WORKDIR /app
ENV TZ=Etc/UTC
COPY . /app/
USER 65532:65532
EXPOSE 8443
ENTRYPOINT ["/app/bin/olo-business-sandbox"]
