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
    implementation(libs.bouncycastle.pkix)
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
    systemProperty("toolgate.fixtures", rootProject.file("tests/fixtures/contracts/v1/valid.json").absolutePath)
    // A fresh PostgreSQL boundary is supplied for each verification invocation.
    outputs.upToDateWhen { false }
}

val consoleDirectory = rootProject.file("apps/admin-ui/dist")
val prebuiltConsole = providers.gradleProperty("prebuiltAdminUi").map(String::toBoolean).getOrElse(false)
val buildAdminUi = tasks.register<Exec>("buildAdminUi") {
    workingDir(rootProject.projectDir)
    commandLine("node", "tools/ui/build.mjs")
    inputs.files(rootProject.fileTree("apps/admin-ui") { exclude("dist/**", "node_modules/**", "coverage/**", "test-results/**") })
    inputs.files(rootProject.fileTree("packages/contracts/typescript/src"), rootProject.file("package-lock.json"), rootProject.file("VERSION"), rootProject.file("LICENSE"), rootProject.file("tools/ui/build.mjs"))
    outputs.dir(consoleDirectory)
    enabled = !prebuiltConsole
}

abstract class VerifyConsoleArtifact : DefaultTask() {
    @get:InputFile abstract val metadata: RegularFileProperty
    @get:Input abstract val releaseVersion: Property<String>
    @get:Input abstract val contractVersion: Property<String>
    @TaskAction fun verify() {
        val content = metadata.get().asFile.readText()
        check(content.contains("\"version\": \"${releaseVersion.get()}\"") && content.contains("\"contracts\": \"${contractVersion.get()}\"")) {
            "Admin UI artifact version mismatch; run node tools/ui/build.mjs"
        }
    }
}
val verifyAdminUi = tasks.register<VerifyConsoleArtifact>("verifyAdminUi") {
    dependsOn(buildAdminUi)
    metadata.set(consoleDirectory.resolve("release.json"))
    releaseVersion.set(rootProject.file("VERSION").readText().trim())
    contractVersion.set(contractsVersion)
}

tasks.processResources {
    dependsOn(verifyAdminUi)
    from(consoleDirectory) { into("META-INF/resources/console"); exclude(".vite/**") }
    from(rootProject.file("packages/contracts/openapi/control-v1.yaml")) {
        into("META-INF")
        rename { "openapi.yaml" }
    }
}
