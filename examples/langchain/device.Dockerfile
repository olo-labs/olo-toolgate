# Copyright 2026 OLO Labs
# SPDX-License-Identifier: Apache-2.0
ARG QUICKSTART_IMAGE=ololab/olo-toolgate-quickstart:dev
FROM ${QUICKSTART_IMAGE} AS release
FROM python:3.14-slim AS extract
WORKDIR /work
COPY --from=release /opt/toolgate/client-downloads/manifest.json /work/assets/
COPY --from=release /opt/toolgate/client-downloads/*-x86_64-unknown-linux-gnu.tar.gz /work/assets/
COPY --from=release /opt/quickstart/VERSION /work/VERSION
COPY tools/client/extract.py tools/client/package.py /work/tools/client/
RUN python tools/client/extract.py --source /work/assets --target x86_64-unknown-linux-gnu --output /opt/toolgate/client
FROM python:3.14-slim
RUN mkdir -p /etc/systemd/system /etc/olo-toolgate /var/lib/olo-toolgate /usr/local/lib/olo-toolgate \
    && chmod 700 /var/lib/olo-toolgate
COPY --from=extract /opt/toolgate/client /opt/toolgate/client
COPY debug/device-entrypoint.py /opt/toolgate/device_entrypoint.py
COPY debug/device-systemctl.sh /usr/bin/systemctl
COPY examples/langchain/device.py /opt/toolgate/device.py
COPY examples/langchain/fixtures /opt/toolgate/fixtures
COPY LICENSE NOTICE.md /usr/share/licenses/olo-toolgate/
RUN chmod 755 /opt/toolgate/client /usr/bin/systemctl
STOPSIGNAL SIGTERM
ENTRYPOINT ["python", "-u", "/opt/toolgate/device.py"]
