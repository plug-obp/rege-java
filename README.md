# Rege

![Build Status](https://github.com/plug-obp/rege-java/actions/workflows/build-and-publish.yml/badge.svg)
![Tests](https://img.shields.io/badge/tests-481%20passing-success)
![Java](https://img.shields.io/badge/java-23-blue)

A Java 23 implementation of regular expression syntax and semantics with formal language properties.

## Modules

### rege-core
Regular expression syntax model, parser, and semantics including:
- Sealed type hierarchy for expressions (Token, Union, Concatenation, KleeneStar, etc.)
- Right and left-associative parsers
- Algebraic simplification
- Brzozowski derivatives
- Nullability and inhabitation checking

### reader-infra
Generic parsing infrastructure for building text parsers:
- Result monad pattern for type-safe error handling
- Shared zero-based source coordinates (UTF-16 columns and offsets, half-open ranges)
- LSP-compatible error reporting
- Zero dependencies

Version **2.0.0** corrects reader-infra's one-based line and
column contract in 1.x to match LSP, and supports diagnostics without source
locations. See the [migration notes](reader-infra/README.md#migrating-from-1x-to-200)
before updating consumers. The repository versions all modules together.

## Build

```bash
./gradlew build
```

## Installation

Add to your `build.gradle`:

```gradle
repositories {
    maven {
        url = uri("https://maven.pkg.github.com/plug-obp/rege-java")
        credentials {
            username = project.findProperty("gpr.user") ?: System.getenv("USERNAME")
            password = project.findProperty("gpr.key") ?: System.getenv("TOKEN")
        }
    }
}

dependencies {
    implementation 'org.obpcdl:rege-core:2.0.0'
    implementation 'org.obpcdl:reader-infra:2.0.0'
}
```

**Note:** GitHub Packages requires authentication. See [GitHub's documentation](https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-gradle-registry) for setup.

## Requirements

- Java 23
- Gradle 8.12.1+

## License

MIT License - See LICENSE file for details

## Documentation

- [CI/CD Setup](docs/CI-CD-SETUP.md)
- [Versioning Strategy](docs/VERSIONING.md)
- [2.0.0 Release Notes](docs/releases/2.0.0.md)
- [Architecture Analysis](docs/architectural-analysis.md)
- [rege-core README](rege-core/README.md)
- [reader-infra README](reader-infra/README.md)
