// Copyright 2026 OLO Labs; SPDX-License-Identifier: Apache-2.0
plugins { id("io.ololabs.toolgate.sdk") }
toolgate { descriptorGeneration.set(1) }
tasks.withType<JavaCompile>().configureEach { options.release.set(17) }
sourceSets.test { java.srcDir(rootProject.file("test-support")) }
tasks.test { systemProperty("knowledge.package", "rag") }
