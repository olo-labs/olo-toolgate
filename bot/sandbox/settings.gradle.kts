// Copyright 2026 OLO Labs; SPDX-License-Identifier: Apache-2.0
rootProject.name="olo-business-sandbox"
dependencyResolutionManagement {
    repositories {
        maven { url=uri(file("../../build/sdk-repo")) }
        maven { url=uri(file("../../.dev/maven")) }
        mavenCentral()
    }
}
