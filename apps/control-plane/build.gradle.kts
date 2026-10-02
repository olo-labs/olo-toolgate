// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
plugins {
    java
    alias(libs.plugins.quarkus)
}

val usePublishedContracts = providers.gradleProperty("usePublishedContracts").map(String::toBoolean).orElse(false)
val contractsVersion = providers.gradleProperty("contractsVersion").getOrElse(rootProject.file("packages/contracts/VERSION").readText().trim())
dependencies {
    implementation(enforcedPlatform(libs.quarkus.bom))
    listOf("rest-jackson", "jdbc-postgresql", "flyway", "smallrye-jwt", "smallrye-health",
        "micrometer-registry-prometheus", "opentelemetry", "logging-json", "smallrye-openapi")
        .forEach { implementation("io.quarkus:quarkus-$it") }
    implementation("org.flywaydb:flyway-database-postgresql")
    implementation(libs.jackson.yaml)
    implementation(libs.json.schema)
    if (usePublishedContracts.get()) {
        implementation("io.ololabs.toolgate:toolgate-contracts:$contractsVersion")
    } else {
        implementation(project(":contracts-java"))
    }
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.launcher)
}

tasks.test {
    systemProperty("toolgate.published", usePublishedContracts.get())
    // A fresh PostgreSQL boundary is supplied for each verification invocation.
    outputs.upToDateWhen { false }
}

tasks.processResources {
    from(rootProject.file("packages/contracts/openapi/control-v1.yaml")) {
        into("META-INF")
        rename { "openapi.yaml" }
    }
}
