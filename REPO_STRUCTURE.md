# Repository Structure

```text
olo-toolgate/
├── apps/
│   ├── gateway/
│   ├── control-plane/
│   ├── admin-ui/
│   ├── endpoint-client/
│   ├── marketplace-api/
│   ├── marketplace-worker/
│   └── marketplace-drupal/
├── backend-libs/
├── crates/
├── packages/
│   └── schemas/
├── templates/
├── deploy/
├── docs/
├── examples/
├── tests/
├── tools/
├── .github/
├── README.md
├── CONTRIBUTING.md
├── DEVELOPMENT.md
├── ARCHITECTURE.md
├── VISION.md
├── ROADMAP.md
├── SECURITY.md
├── SUPPORT.md
├── GOVERNANCE.md
├── CODE_OF_CONDUCT.md
├── CHANGELOG.md
├── LICENSE
├── Makefile
├── settings.gradle.kts
├── build.gradle.kts
├── gradle.properties
├── gradlew
├── gradlew.bat
├── gradle/
├── .env.example
├── .gitignore
├── .dockerignore
├── .editorconfig
└── .gitattributes
```

This first commit is intentionally documentation/architecture-first. Component code should be initialized through small, reviewable PRs while keeping the documented boundaries intact.
