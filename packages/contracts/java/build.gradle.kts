plugins {
    `java-library`
    `maven-publish`
}

description = "Shared protocol and data contracts for OLO ToolGate"

java {
    withSourcesJar()
    withJavadocJar()
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            artifactId = "toolgate-contracts"

            pom {
                name.set("OLO ToolGate Contracts")
                description.set(project.description)
                url.set("https://github.com/olo-labs/olo-toolgate")

                licenses {
                    license {
                        name.set("Apache License 2.0")
                        url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }

                scm {
                    connection.set("scm:git:https://github.com/olo-labs/olo-toolgate.git")
                    developerConnection.set("scm:git:ssh://git@github.com/olo-labs/olo-toolgate.git")
                    url.set("https://github.com/olo-labs/olo-toolgate")
                }
            }
        }
    }

    repositories {
        // Remote publication repositories are intentionally configured later
        // through protected CI credentials. `publishToMavenLocal` works now.
    }
}
