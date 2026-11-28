// Copyright 2026 OLO Labs; SPDX-License-Identifier: Apache-2.0
plugins { application }
group="io.ololabs.bots"
version="0.1.0"
val toolgateVersion=file("../../VERSION").readText().trim()
dependencies {
    implementation("io.github.olo-labs:toolgate-service-authorization:$toolgateVersion")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.20.1")
    runtimeOnly("org.postgresql:postgresql:42.7.13")
    testImplementation("org.junit.jupiter:junit-jupiter:5.14.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.14.2")
}
application { mainClass.set("io.ololabs.bots.sandbox.TargetServer") }
tasks.withType<JavaCompile>().configureEach { options.release.set(21) }
tasks.test { useJUnitPlatform();systemProperty("user.timezone","UTC");outputs.upToDateWhen { false } }
