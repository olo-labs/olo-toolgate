# Copyright 2026 OLO Labs
# SPDX-License-Identifier: Apache-2.0
SHELL := /bin/sh
PYTHON ?= python3

.PHONY: help java-check java-projects publish-contracts-local dev stop check test integration e2e policy-e2e security benchmark containers client docs contracts contracts-check contracts-release-dry-run clean tree

help:
	@printf '%s\n' \
	  'OLO ToolGate repository commands' \
	  '' \
	  '  make dev          Start the contributor development environment' \
	  '  make stop         Stop the development environment' \
	  '  make check        Run formatting/lint/schema/docs checks' \
	  '  make test         Run unit tests' \
	  '  make integration  Run integration tests' \
	  '  make e2e          Run end-to-end tests' \
	  '  make security     Run security test suite' \
	  '  make benchmark    Run benchmark suite' \
	  '  make containers   Build Docker images' \
	  '  make client       Build endpoint client' \
	  '  make docs         Validate documentation' \
	  '  make contracts    Validate shared contract packages' \
	  '  make contracts-release-dry-run  Show contract publication plan' \
	  '  make tree         Print repository structure' \
	  '  make clean        Remove local generated state' \
	  '' \
	  'Implementation is currently scaffold/pre-alpha.'

dev:
	@./tools/dev/dev.sh

stop:
	@./tools/dev/stop.sh

check:
	@$(PYTHON) tools/check.py

test:
	@$(PYTHON) -m unittest discover -s tests/contracts -v
	@$(PYTHON) tools/check.py --gateway-only

integration:
	@$(PYTHON) tools/check.py --publication-only
	@$(PYTHON) tools/check.py --gateway-only

e2e:
	@$(PYTHON) tools/ui/check.py
	@$(PYTHON) tools/policy/check.py --build

policy-e2e:
	@$(PYTHON) tools/policy/check.py --build

security:
	@$(PYTHON) tools/check.py --scans

benchmark:
	@$(PYTHON) tools/gateway/benchmark.py

containers:
	@$(PYTHON) tools/gateway/container.py

client:
	@./tools/dev/not-implemented.sh "endpoint client build"

docs:
	@./tools/dev/docs-check.sh

contracts: contracts-check

contracts-check:
	@$(PYTHON) tools/check.py --contracts-only

contracts-release-dry-run:
	@$(PYTHON) tools/release/bundle.py

tree:
	@find . -path './.git' -prune -o -print | sort

clean:
	@rm -rf .dev .data logs tmp temp reports benchmark-results
	@printf '%s\n' 'Local generated state removed.'

java-projects:
	@./gradlew projects

java-check:
	@./gradlew javaCheck

publish-contracts-local:
	@./gradlew :contracts-java:publishToMavenLocal
