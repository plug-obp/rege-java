# Versioning Strategy

## Overview

This project uses **intelligent branch-based versioning** that automatically creates unique version numbers for every build while keeping clean versions for releases.

## Version Format

```
{base-version}-{branch-name}.{build-number}.{commit-hash}
```

### Examples

| Context | Version | Artifact |
|---------|---------|----------|
| **Release (tagged)** | `1.0.0` | `reader-infra-1.0.0.jar` |
| **Main branch CI** | `1.0.0-main.42.a3f9c21` | `reader-infra-1.0.0-main.42.a3f9c21.jar` |
| **Feature branch CI** | `1.0.0-feature-auth.15.b7e2d4f` | `reader-infra-1.0.0-feature-auth.15.b7e2d4f.jar` |
| **Local development** | `1.0.0-main.53af35e` | `reader-infra-1.0.0-main.53af35e.jar` |

## Configuration

### Base Version

Defined in `gradle.properties`:

```properties
version=1.0.0
```

### Dynamic Suffix

Automatically computed in `build.gradle`:

- **Tagged release**: No suffix (clean version)
- **CI build**: `-{branch}.{run-number}.{commit-hash}`
- **Local build**: `-{branch}.{commit-hash}`

## Usage

### Check Current Version

```bash
./gradlew properties -q | grep "^version:"
```

Output examples:
- `version: 1.0.0` (if tagged)
- `version: 1.0.0-main.42.a3f9c21` (CI build)
- `version: 1.0.0-main.53af35e` (local)

### Build Locally

```bash
./gradlew build
```

Creates JARs with local version: `1.0.0-{branch}.{commit-hash}`

### Publish Locally

```bash
./gradlew publishToMavenLocal
```

Publishes to `~/.m2/repository/org/obpcdl/{artifact}/{version}/`

## Release Process

### 1. Update Version (if needed)

Edit `gradle.properties`:

```properties
version=2.0.0
```

Commit:

```bash
git add gradle.properties
git commit -m "Bump version to 2.0.0"
git push origin main
```

### 2. Create Tag

**Important**: Tag must match gradle.properties version!

```bash
git tag -a v2.0.0 -m "Release version 2.0.0"
git push origin v2.0.0
```

### 3. Automatic Validation

CI automatically validates tag matches gradle.properties:

✅ **Valid**: Tag `v1.0.0` + gradle.properties `version=1.0.0` → Publishes `1.0.0`

❌ **Invalid**: Tag `v2.0.0` + gradle.properties `version=1.0.0` → Build fails with error

### 4. Published Artifacts

**GitHub Packages**: `https://maven.pkg.github.com/plug-obp/rege-java`

**GitHub Release**: `https://github.com/plug-obp/rege-java/releases/tag/v2.0.0`

## Development Workflow

### Normal Development

```bash
# Make changes
git add .
git commit -m "Add feature"
git push origin main
```

**Result**: Automatically publishes `1.0.0-main.{run-number}.{commit-hash}`

### Feature Branches

```bash
git checkout -b feature/auth
# Make changes
git commit -am "Implement auth"
git push origin feature/auth
```

**Result**: Automatically publishes `1.0.0-feature-auth.{run-number}.{commit-hash}`

### Testing Specific Builds

Consumers can reference any published version:

```gradle
dependencies {
    // Stable release
    implementation 'org.obpcdl:reader-infra:1.0.0'
    
    // Latest main branch (build 42, commit a3f9c21)
    implementation 'org.obpcdl:reader-infra:1.0.0-main.42.a3f9c21'
    
    // Specific feature branch (build 15, commit b7e2d4f)
    implementation 'org.obpcdl:reader-infra:1.0.0-feature-auth.15.b7e2d4f'
}
```

## Benefits

✅ **Unique versions**: Every build has a unique identifier (branch + build# + commit)  
✅ **Traceability**: Know exactly which branch, build number, and commit produced an artifact  
✅ **Parallel development**: Multiple branches don't conflict  
✅ **Clean releases**: Tagged releases get professional version numbers  
✅ **Maven compatible**: Branch names automatically sanitized  
✅ **No manual work**: Everything automated via CI/CD  
✅ **Git integration**: Can checkout exact commit that produced any artifact  

## Validation

### Version Validation Task

```bash
./gradlew validateTagVersion
```

**Local (non-tagged)**:
```
ℹ️  Not a tagged release, skipping version validation
```

**Tagged release (valid)**:
```
✅ Version validation passed: 1.0.0
```

**Tagged release (mismatch)**:
```
❌ Version mismatch!
   Git tag version:    2.0.0
   Gradle version:     1.0.0
```

## Branch Name Sanitization

Branch names are sanitized for Maven compatibility:

| Original | Sanitized | Version |
|----------|-----------|---------|
| `main` | `main` | `1.0.0-main.42.a3f9c21` |
| `feature/auth` | `feature-auth` | `1.0.0-feature-auth.15.b7e2d4f` |
| `fix/parser-bug` | `fix-parser-bug` | `1.0.0-fix-parser-bug.8.c9d1e2a` |
| `FEATURE-123` | `feature-123` | `1.0.0-feature-123.7.f4a8b3c` |

**Rules**:
- `/` and `\` → `-`
- Invalid chars → `-`
- Multiple `-` → single `-`
- Lowercase

## Environment Variables (CI)

Used by GitHub Actions:

- `GITHUB_REF_NAME`: Branch or tag name
- `GITHUB_REF_TYPE`: `branch` or `tag`
- `GITHUB_RUN_NUMBER`: Sequential build number
- `GITHUB_REPOSITORY`: Owner/repo name
- `GITHUB_SHA`: Full commit hash (first 7 chars used)

## See Also

- [CI-CD-SETUP.md](CI-CD-SETUP.md) - Complete CI/CD documentation
- [gradle.properties](gradle.properties) - Version configuration
- [build.gradle](build.gradle) - Versioning logic
- [.github/workflows/build-and-publish.yml](.github/workflows/build-and-publish.yml) - CI workflow

---

**Last Updated**: October 2025
