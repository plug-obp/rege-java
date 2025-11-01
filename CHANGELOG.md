# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [1.1.0] - 2025-11-02

### Added

- **New Module**: `rege-modelchecker` - A regular expression model-checker for verifying properties
  - `StepModelChecker` class for model checking with semantic relations
  - Integration with OBP3 model checking framework
  - Support for atomic proposition evaluation
  - Configurable traversal algorithms (DFS) and depth bounds
  - `DependentSemantics` implementation for property checking
- Java Platform Module System (JPMS) support
  - Added `module-info.java` to `reader-infra` module
  - Added `module-info.java` to `rege-core` module
  - Added `module-info.java` to `rege-modelchecker` module

### Changed

- Refactored build configuration
  - Consolidated publishing configuration in root `build.gradle`
  - Improved multi-module Gradle setup
  - Automatic git repository extraction for publishing
  - Enhanced versioning with git hash metadata and local build support
- Updated CI/CD pipeline
  - Refined artifact publishing patterns (`*/build/libs/*.jar`)
  - Added token and actor configuration
- Updated Gradle to version 9.1.0

### Fixed

- Minor code quality improvements in `RegeDependentSemantics`
- Updated `DeepAbsorptionDemo` for better demonstration clarity
- Added javadoc documentation to all `module-info.java` files to eliminate javadoc warnings

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

[Unreleased]: https://github.com/plug-obp/rege-java/compare/v1.1.0...HEAD
[1.1.0]: https://github.com/plug-obp/rege-java/compare/v1.0.0...v1.1.0
[1.0.0]: https://github.com/plug-obp/rege-java/releases/tag/v1.0.0
