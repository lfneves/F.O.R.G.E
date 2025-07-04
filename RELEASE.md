# Release Guide

This document provides instructions for creating and publishing releases of FORGE.

## Release Process

### 1. Prerequisites

Ensure you have:
- JDK 21 installed
- Gradle 8.0+ configured
- Git access to the repository
- Publishing credentials (if publishing to external repositories)

### 2. Preparing a Release

#### Update Version

1. Update version in `build.gradle.kts`:
```kotlin
version = "1.0.0" // FORGE initial release
```

2. Update version in `src/main/resources/version.properties`:
```properties
version=1.0.0
build.date=2025-07-04
```

#### Update Documentation

1. Update `CHANGELOG.md` with release notes
2. Update `README.md` with new version numbers
3. Review and update any version-specific documentation

### 3. Building the Release

#### Local Build
```bash
# Clean and build
./gradlew clean build

# Create release artifacts
./gradlew release

# Verify artifacts
ls -la build/libs/
```

#### Expected Artifacts
- `forge-1.0.0.jar` - Main library
- `forge-1.0.0-sources.jar` - Source code
- `forge-1.0.0-javadoc.jar` - Documentation

### 4. Testing the Release

#### Run Tests
```bash
./gradlew test
```

#### Test Examples
```bash
# Test basic example
./gradlew run -PmainClass=com.forge.examples.basic.BasicForgeExample

# Test virtual threads example
./gradlew run -PmainClass=com.forge.examples.virtualthreads.VirtualThreadExample

# Test Spring Boot integration
./gradlew bootRun
```

#### Verify JAR
```bash
# Check JAR contents
jar -tf build/libs/forge-1.0.0.jar | head -20

# Verify version
java -cp build/libs/forge-1.0.0.jar -version
```

### 5. Publishing the Release

#### Local Repository
```bash
./gradlew publishToMavenLocal
```

#### External Repository
```bash
# Configure credentials in gradle.properties or environment variables
./gradlew publish
```

#### GitHub Packages
```bash
# Requires GitHub token
export USERNAME=your-github-username
export TOKEN=your-github-token
./gradlew publishRelease
```

### 6. Creating Git Release

#### Tag the Release
```bash
git tag -a v1.0.0 -m "FORGE v1.0.0 - Initial Release"
git push origin v1.0.0
```

#### GitHub Release
1. Go to GitHub repository
2. Click "Releases" → "Create a new release"
3. Select tag `v1.0.0`
4. Upload artifacts from `build/libs/`
5. Copy release notes from `CHANGELOG.md`
6. Publish release

### 7. Post-Release Tasks

#### Update to Next Development Version
```kotlin
// In build.gradle.kts
version = "1.0.1-SNAPSHOT"
```

#### Communication
- Announce release on relevant channels
- Update documentation websites
- Notify users and contributors

## Release Checklist

### Pre-Release
- [ ] All tests passing
- [ ] Documentation updated
- [ ] Version numbers updated
- [ ] CHANGELOG.md updated
- [ ] Examples working
- [ ] Dependencies up to date

### Release Build
- [ ] Clean build successful
- [ ] All artifacts generated
- [ ] JAR verification complete
- [ ] Examples tested with release JARs
- [ ] Performance benchmarks run

### Publishing
- [ ] Local repository publish
- [ ] External repository publish (if applicable)
- [ ] Git tag created
- [ ] GitHub release created
- [ ] Artifacts uploaded

### Post-Release
- [ ] Next version number set
- [ ] Release announcement made
- [ ] Documentation sites updated
- [ ] Issues/PRs updated with version info

## Gradle Release Commands

### Available Tasks

```bash
# Clean and build everything
./gradlew clean build

# Create release build
./gradlew release

# Publish to repositories
./gradlew publishRelease

# Show all release-related tasks
./gradlew tasks --group release
```

### Custom Release Properties

Create `gradle.properties` for custom settings:

```properties
# Publishing
gpr.user=your-username
gpr.key=your-token

# Release settings
release.skipTests=false
release.pushTag=true
```

## Troubleshooting

### Common Issues

1. **Build Failures**
   - Ensure JDK 21 is active
   - Clean gradle cache: `./gradlew clean`
   - Check dependency conflicts

2. **Publishing Issues**
   - Verify credentials
   - Check repository permissions
   - Ensure network connectivity

3. **Test Failures**
   - Run tests individually to isolate issues
   - Check for environment-specific problems
   - Verify virtual threads support

### Support

For release-related issues:
- Check the [FAQ](https://github.com/lfneves/forge/wiki/FAQ)
- Open an [issue](https://github.com/lfneves/forge/issues)
- Contact the maintainers

## Automated Release

### GitHub Actions (Future)

```yaml
# .github/workflows/release.yml
name: Release
on:
  push:
    tags: ['v*']
jobs:
  release:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Setup JDK 21
        uses: actions/setup-java@v3
        with:
          java-version: '21'
      - name: Build Release
        run: ./gradlew release
      - name: Upload Artifacts
        uses: actions/upload-artifact@v3
        with:
          name: forge-artifacts
          path: build/libs/
```