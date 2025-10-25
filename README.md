# Rege

![Build Status](https://github.com/plug-obp/rege-java/actions/workflows/build-and-publish.yml/badge.svg)
![Tests](https://img.shields.io/badge/tests-462%20passing-success)
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
- Position tracking (line, column, offset)
- LSP-compatible error reporting
- Zero dependencies

## Build

```bash
./gradlew build
```

## Requirements

- Java 23
- Gradle 8.12.1+

## License

MIT License - See LICENSE file for details

## Documentation

- [CI/CD Setup](docs/CI-CD-SETUP.md)
- [Versioning Strategy](docs/VERSIONING.md)
- [Architecture Analysis](docs/architectural-analysis.md)
- [rege-core README](rege-core/README.md)
- [reader-infra README](reader-infra/README.md)
