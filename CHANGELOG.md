# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [1.0.0] - 2025-10-25

### Added

- Initial release of rege-java
- Core regular expression syntax model with sealed types hierarchy
  - `Expression` sealed interface with implementations: `Token`, `Union`, `Concatenation`, `KleeneStar`, `Complement`, `Intersection`
  - Support for both `EMPTY` (ε) and `UNIVERSAL` (Σ*) constants
- Two parser implementations:
  - `RegeReader` - Smart parser with automatic simplification
  - `RegeReaderLeft` - Left-biased parser for syntactic analysis
- Type-safe error handling with `ParseResult<T>` (sealed interface with `Success` and `Failure` records)
- Comprehensive syntax validation:
  - `AlienValidator` for custom token validation
  - `SyntaxValidator` for expression validation
- Semantic operations:
  - `Nullability` - Check if a regular expression accepts the empty string
  - `Inhabitation` - Check if a regular expression accepts any string
  - `Brzozowski` - Compute derivatives of regular expressions
- Expression simplification:
  - `Simplifier` with deep absorption and normalization rules
  - Configurable simplification strategies
- Pretty printing with `PrettyPrinter`
- Reader infrastructure (`reader-infra` module) for extensible parsing
- Complete test suite with 355 passing tests
- Comprehensive JavaDoc documentation
- CI/CD pipeline with GitHub Actions
  - Automated builds on push and pull requests
  - Dynamic versioning: `branch.build.commit` format
  - Test execution and reporting
- Build configuration:
  - Gradle multi-module setup
  - Java 23 with preview features enabled
  - JUnit 5 for testing

### Documentation

- README.md with usage examples and API overview
- Architecture analysis document (Grade: A+)
- CI/CD setup documentation
- Versioning strategy documentation
- Package-level JavaDoc with usage examples

[Unreleased]: https://github.com/plug-obp/rege-java/compare/v1.0.0...HEAD
[1.0.0]: https://github.com/plug-obp/rege-java/releases/tag/v1.0.0
