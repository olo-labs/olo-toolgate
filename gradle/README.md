# Gradle Bootstrap

OLO ToolGate uses **Gradle Kotlin DSL** for Java modules.

Pinned Gradle version:

```text
9.7.1
```

The canonical wrapper properties are in:

```text
gradle/wrapper/gradle-wrapper.properties
```

## First-Commit Bootstrap

The first scaffold includes a checksum-verifying `gradlew`/`gradlew.bat` bootstrap so contributors can run Gradle before the standard generated `gradle-wrapper.jar` is committed.

Once a machine has Gradle available, maintainers should generate and commit the official wrapper:

```bash
./gradlew wrapper --gradle-version 9.7.1 --distribution-type bin
```

Then verify the generated wrapper JAR against the official Gradle wrapper checksum before committing it.

Expected Gradle 9.7.1 checksums:

```text
Binary distribution:
acd53f1edaf02f1a8ff99879f8a34b302661a057d9b063ae9e35b552f804d20a

Wrapper JAR:
7a9ce74cff467ca1bf60a4fcd9f05185acceda4d0f382434d393e17864262c5d
```

The custom bootstrap scripts may then be replaced by Gradle's generated standard scripts.
