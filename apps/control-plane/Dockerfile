# Copyright 2026 OLO Labs
# SPDX-License-Identifier: Apache-2.0
FROM eclipse-temurin:21-jdk-noble@sha256:70898f0f893a6b772a0f29834d8b022e3ac20b6a0c33a922973cf66342ef56be AS build
WORKDIR /workspace
RUN apt-get update && apt-get install -y --no-install-recommends unzip && rm -rf /var/lib/apt/lists/*
COPY gradlew gradlew.bat settings.gradle.kts build.gradle.kts gradle.properties VERSION ./
COPY gradle/ gradle/
COPY packages/contracts/ packages/contracts/
COPY apps/control-plane/ apps/control-plane/
COPY apps/marketplace-api/build.gradle.kts apps/marketplace-api/build.gradle.kts
COPY apps/marketplace-worker/build.gradle.kts apps/marketplace-worker/build.gradle.kts
RUN --mount=type=cache,target=/root/.gradle chmod +x gradlew && ./gradlew --no-daemon :control-plane:quarkusBuild

FROM eclipse-temurin:21-jre-noble@sha256:22138efd69393501fccd8176ae16b01791ed71ff801b28f0359415389b17c766
ARG VERSION
ARG REVISION
LABEL org.opencontainers.image.title="OLO ToolGate Control Plane" \
      org.opencontainers.image.source="https://github.com/olo-labs/olo-toolgate" \
      org.opencontainers.image.licenses="Apache-2.0" \
      org.opencontainers.image.version=$VERSION \
      org.opencontainers.image.revision=$REVISION
WORKDIR /app
COPY --from=build --chown=65532:65532 /workspace/apps/control-plane/build/quarkus-app/ /app/
COPY LICENSE apps/control-plane/NOTICE.md /usr/share/licenses/olo-toolgate/
COPY apps/control-plane/gradle.lockfile /usr/share/licenses/olo-toolgate/third-party/gradle.lockfile
COPY tools/dependency-license-reviews.json /usr/share/licenses/olo-toolgate/third-party/
USER 65532:65532
EXPOSE 8082 9092
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=70 -Djava.io.tmpdir=/tmp"
ENTRYPOINT ["java", "-jar", "/app/quarkus-run.jar"]
