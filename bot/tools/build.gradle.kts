// Copyright 2026 OLO Labs; SPDX-License-Identifier: Apache-2.0
plugins { id("io.ololabs.toolgate.sdk") apply false }
subprojects {
    group = "io.ololabs.bots"
    version = "0.1.0"
    plugins.withId("io.ololabs.toolgate.sdk") {
        val isolatedImage = providers.gradleProperty("toolgateJvmImageDigest")
            .orElse(providers.environmentVariable("TOOLGATE_JVM_IMAGE_DIGEST"))
        extensions.configure<io.ololabs.toolgate.gradle.ToolGateExtension> {
            baseImageDigest.set(isolatedImage)
        }
        tasks.named("toolgatePackage") {
            doFirst {
                require(isolatedImage.isPresent) {
                    "Starter packages require -PtoolgateJvmImageDigest=<qualified OCI image digest>"
                }
            }
        }
        dependencies.add("testImplementation", "org.junit.jupiter:junit-jupiter:5.14.2")
        dependencies.add("testRuntimeOnly", "org.junit.platform:junit-platform-launcher:1.14.2")
        tasks.withType<Test>().configureEach { useJUnitPlatform() }
    }
}
