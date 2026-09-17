import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.testing.Test
import org.gradle.jvm.toolchain.JavaLanguageVersion

plugins {
    base
}

group = "io.ololabs.toolgate"
version = providers.gradleProperty("toolgateVersion").get()

allprojects {
    group = rootProject.group
    version = rootProject.version

    repositories {
        mavenCentral()
        mavenLocal()
    }
}

subprojects {
    plugins.withType<JavaPlugin> {
        extensions.configure<JavaPluginExtension> {
            toolchain {
                languageVersion.set(JavaLanguageVersion.of(21))
            }
        }

        tasks.withType<JavaCompile>().configureEach {
            options.encoding = "UTF-8"
            options.compilerArgs.addAll(listOf("-Xlint:all", "-Werror"))
        }

        tasks.withType<Test>().configureEach {
            useJUnitPlatform()
        }
    }
}

tasks.register("javaCheck") {
    group = "verification"
    description = "Runs checks for all Java/Gradle modules."
    dependsOn(
        ":contracts-java:check",
        ":control-plane:check",
        ":marketplace-api:check",
        ":marketplace-worker:check",
    )
}

tasks.register("publishContractsToMavenLocal") {
    group = "publishing"
    description = "Publishes the shared Java contracts to the local Maven repository."
    dependsOn(":contracts-java:publishToMavenLocal")
}
