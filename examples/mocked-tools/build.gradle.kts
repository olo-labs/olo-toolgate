// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
plugins {
    // The SDK version every example builds against; each example applies the plugin without a version.
    id("io.ololabs.toolgate.sdk") apply false
}

subprojects {
    group = "io.ololabs.examples.mocked"
    val isolatedImage = providers.gradleProperty("toolgateJvmImageDigest")
    version = if (isolatedImage.isPresent) {
        providers.gradleProperty("toolgatePackageVersion").orNull
            ?: error("Isolated packages require -PtoolgatePackageVersion=<new release version>")
    } else "0.1.0"
    plugins.withId("io.ololabs.toolgate.sdk") {
        extensions.configure<io.ololabs.toolgate.gradle.ToolGateExtension> {
            baseImageDigest.set(isolatedImage)
        }
    }
}
