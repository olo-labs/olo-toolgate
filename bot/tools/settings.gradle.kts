// Copyright 2026 OLO Labs; SPDX-License-Identifier: Apache-2.0
pluginManagement {
    plugins {
        id("io.ololabs.toolgate.sdk") version file("../../VERSION").readText().trim()
    }
    repositories {
        maven { url = uri(providers.gradleProperty("toolgateSdkRepo").orElse(file("../../build/sdk-repo").absolutePath).get()) }
        mavenLocal()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositories {
        maven { url = uri(providers.gradleProperty("toolgateSdkRepo").orElse(file("../../build/sdk-repo").absolutePath).get()) }
        mavenLocal()
        mavenCentral()
    }
}
rootProject.name = "olo-business-tools"
include("business-workflows")
include("rag", "memory")
include("sandbox")
