rootProject.name = "olo-toolgate"

include(":contracts-java")
project(":contracts-java").projectDir = file("packages/contracts/java")

include(":control-plane")
project(":control-plane").projectDir = file("apps/control-plane")

include(":marketplace-api")
project(":marketplace-api").projectDir = file("apps/marketplace-api")

include(":marketplace-worker")
project(":marketplace-worker").projectDir = file("apps/marketplace-worker")
