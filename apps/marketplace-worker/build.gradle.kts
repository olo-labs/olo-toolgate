plugins {
    `java-library`
}

val usePublishedContracts = providers
    .gradleProperty("usePublishedContracts")
    .map(String::toBoolean)
    .orElse(false)

val contractsVersion = providers
    .gradleProperty("contractsVersion")
    .orElse(rootProject.version.toString())

dependencies {
    if (usePublishedContracts.get()) {
        implementation("io.ololabs.toolgate:toolgate-contracts:${contractsVersion.get()}")
    } else {
        implementation(project(":contracts-java"))
    }
}
