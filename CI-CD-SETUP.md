# CI/CD Setup Documentation

## Overview

This document describes the Continuous Integration and Continuous Deployment (CI/CD) pipeline configured for the rege-java project using GitHub Actions and GitHub Packages with **dynamic branch-based versioning**.

## Versioning Strategy

### Dynamic Version Format

The project uses **intelligent branch-based versioning** that automatically adapts based on the build context:

| Build Context | Version Format | Example |
|--------------|----------------|---------|
| **Local development** | `{version}-{branch}.{commit}` | `1.0.0-main.a3f9c21` |
| **CI build (main)** | `{version}-{branch}.{run}.{commit}` | `1.0.0-main.42.a3f9c21` |
| **CI build (feature)** | `{version}-{branch}.{run}.{commit}` | `1.0.0-feature-auth.15.b7e2d4f` |
| **Tagged release** | `{version}` (clean) | `1.0.0` |

### Benefits

✅ **Traceability**: Instantly identify which branch and build produced an artifact  
✅ **Parallel Development**: Different branches produce distinct versions  
✅ **No Collisions**: Each build has a unique version number  
✅ **Clean Releases**: Tagged releases get clean version numbers  
✅ **Maven Compatible**: Branch names sanitized for Maven repository requirements  

### Version Configuration

**Base version** is defined in `gradle.properties`:
```properties
version=1.0.0
```

**Dynamic suffix** is computed automatically:
- **Tagged release (`v1.0.0`)**: No suffix → `1.0.0`
- **Main branch build #42**: `-main.42.a3f9c21` → `1.0.0-main.42.a3f9c21`
- **Feature branch build #15**: `-feature-auth.15.b7e2d4f` → `1.0.0-feature-auth.15.b7e2d4f`
- **Local build on main**: `-main.{git-hash}` → `1.0.0-main.53af35e`

### Branch Name Sanitization

Branch names are automatically sanitized for Maven compatibility:

| Original Branch | Sanitized | Final Version |
|----------------|-----------|---------------|
| `main` | `main` | `1.0.0-main.42.a3f9c21` |
| `feature/auth` | `feature-auth` | `1.0.0-feature-auth.15.b7e2d4f` |
| `fix/parser-bug` | `fix-parser-bug` | `1.0.0-fix-parser-bug.8.c9d1e2a` |
| `user@name/test` | `user-name-test` | `1.0.0-user-name-test.3.e5f7a9b` |

**Sanitization rules:**
- Slashes (`/`, `\`) → dashes (`-`)
- Invalid characters → dashes
- Consecutive dashes → single dash
- Converted to lowercase

## Architecture

### GitHub Actions Workflow

**File:** `.github/workflows/build-and-publish.yml`

The workflow consists of two main jobs:

1. **Build Job** - Runs on **all branches, PRs, and tags** (provides feedback everywhere)
2. **Publish Job** - Runs **selectively** on `main`/`Main` branch and version tags only (prevents repository bloat)

### Triggers

The workflow is triggered by:
- **Push to ANY branch** - Build and test all code changes
- **All pull requests** - Validate PRs before merge
- **Version tags** (`v*`, e.g., `v1.0.0`) - Build and publish releases
- **Manual workflow dispatch** (via GitHub UI)

### Why Build Everything But Publish Selectively?

**Build on all branches:**
- ✅ Immediate feedback on feature branches
- ✅ Catch build breaks before opening PRs
- ✅ Run tests on all code changes
- ✅ Generate artifacts for testing (available as CI artifacts)

**Publish only main + tags:**
- ✅ Avoid GitHub Packages repository bloat
- ✅ Reduce storage costs
- ✅ Keep published artifacts clean and discoverable
- ✅ Only publish "official" snapshots and releases

**Feature branch artifacts:** Available as GitHub Actions artifacts (90-day retention) without cluttering the package repository.

## Build Job

### What It Does

1. **Checkout Code** - Uses `actions/checkout@v4`
2. **Setup Java 23** - Uses `actions/setup-java@v4` with Eclipse Temurin distribution
3. **Build Project** - Runs `./gradlew build`
4. **Run Tests** - Runs `./gradlew test`
5. **Publish Test Results** - Uses `EnricoMi/publish-unit-test-result-action@v2`
6. **Upload Artifacts** - Uploads built JAR files for 30 days

### Artifacts Produced

- `reader-infra-*.jar` - Main JAR
- `reader-infra-*-javadoc.jar` - Javadoc JAR
- `reader-infra-*-sources.jar` - Sources JAR
- `rege-core-*.jar` - Main JAR
- `rege-core-*-javadoc.jar` - Javadoc JAR
- `rege-core-*-sources.jar` - Sources JAR

All artifacts are available in the GitHub Actions UI under "Artifacts" for 30 days.

## Publish Job

### What It Does

1. **Checkout Code**
2. **Setup Java 23**
3. **Publish to GitHub Packages** - Runs `./gradlew publish`
4. **Create GitHub Release** (for version tags only)

### Conditions

The publish job only runs when:
- Push to `main` or `Main` branch, OR
- Push of a version tag (e.g., `v1.0.0`)

### GitHub Packages Repository

Artifacts are published to:
```
https://maven.pkg.github.com/plug-obp/rege-java
```

**Published Packages:**

- `org.obpcdl:reader-infra:{version}` - e.g., `1.0.0-main.42` or `1.0.0`
- `org.obpcdl:rege-core:{version}` - e.g., `1.0.0-main.42` or `1.0.0`

**Version examples:**
- Main branch build: `org.obpcdl:reader-infra:1.0.0-main.42.a3f9c21`
- Feature branch: `org.obpcdl:reader-infra:1.0.0-feature-auth.15.b7e2d4f`
- Tagged release: `org.obpcdl:reader-infra:1.0.0`

## Maven Publishing Configuration

### Gradle Plugin

Both `reader-infra` and `rege-core` modules use the `maven-publish` plugin:

```gradle
plugins {
    id 'java'
    id 'maven-publish'
}
```

### Java Archives

Each module publishes three JAR files:

```gradle
java {
    withJavadocJar()
    withSourcesJar()
}
```

### Publication Configuration

```gradle
publishing {
    publications {
        maven(MavenPublication) {
            groupId = 'org.obpcdl'
            artifactId = 'reader-infra'  // or 'rege-core'
            version = '1.0.0'
            
            from components.java
            
            pom {
                name = 'Reader Infrastructure'
                description = '...'
                url = 'https://github.com/plug-obp/rege-java'
                licenses { ... }
                developers { ... }
                scm { ... }
            }
        }
    }
}
```

### Repository Configuration

```gradle
publishing {
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/\${System.getenv("GITHUB_REPOSITORY")}")
            credentials {
                username = System.getenv("GITHUB_ACTOR")
                password = System.getenv("GITHUB_TOKEN")
            }
        }
    }
}
```

**Authentication:** Uses GitHub Actions environment variables:
- `GITHUB_ACTOR` - The username triggering the workflow
- `GITHUB_TOKEN` - Automatically provided by GitHub Actions
- `GITHUB_REPOSITORY` - The repository in format `owner/repo`

## Using Published Packages

### Version Selection

**For stable releases** (recommended for production):
```gradle
dependencies {
    implementation 'org.obpcdl:reader-infra:1.0.0'
}
```

**For testing specific branch builds**:
```gradle
dependencies {
    // Test main branch build #42, commit a3f9c21
    implementation 'org.obpcdl:reader-infra:1.0.0-main.42.a3f9c21'
    
    // Test feature branch build #15, commit b7e2d4f
    implementation 'org.obpcdl:reader-infra:1.0.0-feature-auth.15.b7e2d4f'
}
```

### For Maven Projects

Add the repository to `pom.xml`:

```xml
<repositories>
    <repository>
        <id>github</id>
        <url>https://maven.pkg.github.com/plug-obp/rege-java</url>
    </repository>
</repositories>
```

Add dependencies:

```xml
<dependency>
    <groupId>org.obpcdl</groupId>
    <artifactId>reader-infra</artifactId>
    <version>1.0.0</version>
</dependency>

<dependency>
    <groupId>org.obpcdl</groupId>
    <artifactId>rege-core</artifactId>
    <version>1.0.0</version>
</dependency>
```

### For Gradle Projects

Add the repository to `build.gradle`:

```gradle
repositories {
    mavenCentral()
    maven {
        url = uri("https://maven.pkg.github.com/plug-obp/rege-java")
        credentials {
            username = project.findProperty("gpr.user") ?: System.getenv("GITHUB_ACTOR")
            password = project.findProperty("gpr.token") ?: System.getenv("GITHUB_TOKEN")
        }
    }
}
```

Add dependencies:

```gradle
dependencies {
    implementation 'org.obpcdl:reader-infra:1.0.0'
    implementation 'org.obpcdl:rege-core:1.0.0'
}
```

### Authentication for Downloads

GitHub Packages requires authentication even for public repositories. Users need:

1. **Personal Access Token (PAT)** with `read:packages` scope
2. Configure credentials:
   - Maven: In `~/.m2/settings.xml`
   - Gradle: In `~/.gradle/gradle.properties` or environment variables

**Example for Gradle** (`~/.gradle/gradle.properties`):
```properties
gpr.user=your-github-username
gpr.token=your-personal-access-token
```

## Local Publishing (Testing)

### Publish to Local Maven Repository

```bash
./gradlew publishToMavenLocal
```

This publishes to `~/.m2/repository/org/obpcdl/` for local testing.

### Dry Run

```bash
./gradlew publish --dry-run
```

Shows what would be published without actually doing it.

### Check Available Tasks

```bash
./gradlew tasks --group=publishing
```

Available publishing tasks:
- `publish` - Publishes all publications to all repositories
- `publishAllPublicationsToGitHubPackagesRepository` - Publish all to GitHub Packages
- `publishMavenPublicationToGitHubPackagesRepository` - Publish Maven publication to GitHub
- `publishToMavenLocal` - Publish to local Maven repository

## Release Process

### Creating a Release

**Prerequisites:**
1. Update version in `gradle.properties` if needed (e.g., `version=2.0.0`)
2. Ensure tag version matches gradle.properties version
3. Commit all changes

**Steps:**

1. **Commit version update** (if changed):
   ```bash
   # Edit gradle.properties: version=2.0.0
   git add gradle.properties
   git commit -m "Bump version to 2.0.0"
   git push origin main
   ```

2. **Create and push tag**:
   ```bash
   # Tag must start with 'v' and match gradle.properties version
   git tag -a v2.0.0 -m "Release version 2.0.0"
   git push origin v2.0.0
   ```

3. **Automated validation and publishing**:
   - GitHub Actions validates tag matches gradle.properties
   - If mismatch: build fails with clear error message
   - If valid: publishes clean version (`2.0.0`) to GitHub Packages
   - Creates GitHub Release with artifacts

### Version Validation

The CI pipeline automatically validates that git tags match the version in `gradle.properties`:

**✅ Valid example:**
```bash
# gradle.properties: version=1.0.0
git tag v1.0.0
# ✅ Publishes: 1.0.0
```

**❌ Invalid example:**
```bash
# gradle.properties: version=1.0.0
git tag v2.0.0
# ❌ Build fails with:
#    Version mismatch!
#      Git tag version:    2.0.0
#      Gradle version:     1.0.0
```

### Development Workflow

**Regular development** (automatic):
```bash
# Make changes
git add .
git commit -m "Add feature"
git push origin main

# Automatically publishes: 1.0.0-main.{run_number}
```

**Feature branches** (automatic):
```bash
git checkout -b feature/auth
# Make changes
git commit -am "Implement auth"
git push origin feature/auth

# Automatically publishes: 1.0.0-feature-auth.{run_number}
```

**Testing locally**:
```bash
./gradlew build
# Creates: reader-infra-1.0.0-main.{git-hash}.jar

./gradlew publishToMavenLocal
# Publishes to: ~/.m2/repository/org/obpcdl/reader-infra/1.0.0-main.{git-hash}/
```

## Version Examples

### Published Artifacts Over Time

| Event | Version Published | Artifact Example |
|-------|------------------|------------------|
| Push to main #1 | `1.0.0-main.1.a1b2c3d` | `reader-infra-1.0.0-main.1.a1b2c3d.jar` |
| Push to main #2 | `1.0.0-main.2.e4f5g6h` | `reader-infra-1.0.0-main.2.e4f5g6h.jar` |
| PR feature/auth #5 | `1.0.0-feature-auth.5.i7j8k9l` | `reader-infra-1.0.0-feature-auth.5.i7j8k9l.jar` |
| Tag `v1.0.0` | `1.0.0` | `reader-infra-1.0.0.jar` |
| Push to main #10 | `1.0.0-main.10.m1n2o3p` | `reader-infra-1.0.0-main.10.m1n2o3p.jar` |
| Update to 2.0.0, tag `v2.0.0` | `2.0.0` | `reader-infra-2.0.0.jar` |

### Release Artifacts

Each GitHub Release includes:
- `reader-infra-1.0.0.jar`
- `reader-infra-1.0.0-javadoc.jar`
- `reader-infra-1.0.0-sources.jar`
- `rege-core-1.0.0.jar`
- `rege-core-1.0.0-javadoc.jar`
- `rege-core-1.0.0-sources.jar`

## Monitoring

### GitHub Actions

View workflow runs at:
```
https://github.com/plug-obp/rege-java/actions
```

Each run shows:
- Build logs
- Test results
- Published artifacts
- Any errors or warnings

### GitHub Packages

View published packages at:
```
https://github.com/plug-obp/rege-java/packages
```

Shows:
- Package versions
- Download statistics
- Package metadata

## Security

### Secrets and Tokens

- `GITHUB_TOKEN` - Automatically provided, scoped to the repository
- No manual secrets required for basic GitHub Packages publishing
- Token has write permissions to packages in the same repository

### Permissions

The workflow requires:
- `contents: write` - For creating releases
- `packages: write` - For publishing to GitHub Packages
- `checks: write` - For publishing test results

These are granted via the `GITHUB_TOKEN` automatically.

## Troubleshooting

### Build Fails

1. Check Java version (must be 23)
2. Check dependencies in `build.gradle`
3. Run locally: `./gradlew clean build`

### Publish Fails

1. Verify `GITHUB_TOKEN` has package write permissions
2. Check repository URL matches actual GitHub repository
3. Verify network connectivity to GitHub Packages

### Download Fails (Users)

1. Verify authentication token has `read:packages` scope
2. Check credentials configuration in Maven/Gradle
3. Verify package exists and version is correct

### Test Results Not Published

1. Check test reports exist in `build/test-results/test/`
2. Verify `EnricoMi/publish-unit-test-result-action@v2` has permissions
3. Check workflow permissions settings

## Future Enhancements

### Potential Improvements

1. **Maven Central Publishing**
   - Requires Sonatype OSSRH account
   - Need GPG signing for artifacts
   - More discoverable than GitHub Packages

2. **Code Coverage Reports**
   - Add JaCoCo plugin
   - Publish coverage to Codecov/Coveralls
   - Add coverage badge to README

3. **Quality Gates**
   - Add SonarQube/SonarCloud analysis
   - Enforce minimum test coverage
   - Static code analysis (SpotBugs, PMD)

4. **Automated Versioning**
   - Semantic versioning from commit messages
   - Automatic version bumping
   - Changelog generation

5. **Multi-Platform Testing**
   - Test on Ubuntu, macOS, Windows
   - Test with multiple Java versions (17, 21, 23)

6. **Performance Benchmarks**
   - JMH benchmarks in CI
   - Track performance regression
   - Publish benchmark results

## References

- [GitHub Actions Documentation](https://docs.github.com/en/actions)
- [GitHub Packages Documentation](https://docs.github.com/en/packages)
- [Gradle Maven Publish Plugin](https://docs.gradle.org/current/userguide/publishing_maven.html)
- [Maven Publishing Guide](https://maven.apache.org/guides/mini/guide-central-repository-upload.html)

## Summary

The CI/CD pipeline is now fully configured and operational:

✅ **Build automation** - Every push triggers build and test  
✅ **Test reporting** - Test results published to GitHub  
✅ **Artifact publishing** - Automated publishing to GitHub Packages  
✅ **Release automation** - GitHub releases created for version tags  
✅ **Multi-module support** - Both reader-infra and rege-core published  
✅ **POM metadata** - Complete project information in published POMs  
✅ **Source and Javadoc JARs** - Published alongside main JARs  

**Status:** Production-ready ✨

---

*Last Updated: 2025*
