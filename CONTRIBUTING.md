# Contributing to WebFramework

Thank you for your interest in contributing to WebFramework! This document provides guidelines and information for contributors.

## Table of Contents
- [Code of Conduct](#code-of-conduct)
- [Getting Started](#getting-started)
- [Development Setup](#development-setup)
- [Contributing Guidelines](#contributing-guidelines)
- [Pull Request Process](#pull-request-process)
- [Issue Reporting](#issue-reporting)
- [Security Issues](#security-issues)
- [Coding Standards](#coding-standards)
- [Testing Guidelines](#testing-guidelines)
- [Documentation](#documentation)

## Code of Conduct

By participating in this project, you agree to abide by our [Code of Conduct](CODE_OF_CONDUCT.md). Please read it before contributing.

## Getting Started

1. **Fork the repository** on GitHub
2. **Clone your fork** locally:
   ```bash
   git clone https://github.com/yourusername/webframework.git
   cd webframework
   ```
3. **Add the upstream remote**:
   ```bash
   git remote add upstream https://github.com/original/webframework.git
   ```
4. **Create a feature branch**:
   ```bash
   git checkout -b feature/your-feature-name
   ```

## Development Setup

### Prerequisites
- **JDK 21+** (required for virtual threads)
- **Gradle 8.0+**
- **Git**
- **IDE** (IntelliJ IDEA recommended)

### Build and Test
```bash
# Build the project
./gradlew build

# Run all tests
./gradlew test

# Run specific test categories
./gradlew test --tests "*Security*"
./gradlew test --tests "*VirtualThread*"

# Generate test coverage report
./gradlew jacocoTestReport
```

### Project Structure
```
src/main/kotlin/com/webframework/
├── core/           # Core framework components
├── security/       # Security framework
├── concurrent/     # Virtual threads support
├── spring/         # Spring Boot integration
└── examples/       # Usage examples

src/test/kotlin/com/webframework/
├── core/           # Core tests
├── security/       # Security tests
└── integration/    # Integration tests
```

## Contributing Guidelines

### Types of Contributions
We welcome contributions in the following areas:

1. **Bug Fixes** - Fix existing issues
2. **New Features** - Add new functionality
3. **Security Improvements** - Enhance security features
4. **Performance Optimizations** - Improve performance
5. **Documentation** - Improve docs and examples
6. **Tests** - Add or improve test coverage

### Before You Start
- **Check existing issues** to avoid duplicate work
- **Discuss major changes** in an issue first
- **Keep changes focused** - one feature/fix per PR
- **Follow coding standards** (see below)

## Pull Request Process

### 1. Prepare Your Changes
```bash
# Keep your fork up to date
git fetch upstream
git checkout main
git merge upstream/main
git push origin main

# Create feature branch
git checkout -b feature/your-feature
```

### 2. Make Your Changes
- Write clean, readable code
- Follow our coding standards
- Add tests for new functionality
- Update documentation as needed
- Ensure all tests pass

### 3. Commit Your Changes
```bash
# Stage your changes
git add .

# Commit with descriptive message
git commit -m "Add JWT refresh token support

- Implement token refresh mechanism
- Add expiration handling
- Update security tests
- Document new API endpoints"
```

### 4. Push and Create PR
```bash
# Push to your fork
git push origin feature/your-feature

# Create pull request on GitHub
```

### 5. PR Requirements
- [ ] Clear description of changes
- [ ] Tests pass (CI will verify)
- [ ] Code follows style guidelines
- [ ] Documentation updated
- [ ] Security implications considered
- [ ] Breaking changes documented

## Issue Reporting

### Bug Reports
Use our [bug report template](.github/ISSUE_TEMPLATE/bug_report.md) and include:
- Clear description
- Steps to reproduce
- Expected vs actual behavior
- Environment details
- Code example
- Logs/stack traces

### Feature Requests
Use our [feature request template](.github/ISSUE_TEMPLATE/feature_request.md) and include:
- Clear description
- Use case and benefits
- Example implementation
- Alternatives considered

### Questions
For questions, use:
- [GitHub Discussions](../../discussions) for general questions
- [Question template](.github/ISSUE_TEMPLATE/question.md) for specific issues

## Security Issues

**Never report security vulnerabilities in public issues.**

For security issues:
1. Use GitHub's private vulnerability reporting
2. Email security@webframework.org (if available)
3. Contact maintainers directly

See our [security policy](.github/ISSUE_TEMPLATE/security_issue.md) for details.

## Coding Standards

### Kotlin Style
- Follow [Kotlin coding conventions](https://kotlinlang.org/docs/coding-conventions.html)
- Use meaningful variable and function names
- Keep functions small and focused
- Add KDoc comments for public APIs

### Example:
```kotlin
/**
 * Creates a new JWT token with the specified claims.
 *
 * @param subject The token subject (usually user ID)
 * @param roles Set of user roles
 * @param permissions Set of user permissions
 * @param expirationMinutes Token expiration time in minutes
 * @return Generated JWT token with claims
 */
fun createToken(
    subject: String,
    roles: Set<String> = emptySet(),
    permissions: Set<String> = emptySet(),
    expirationMinutes: Long? = null
): JWTToken {
    // Implementation...
}
```

### Security Code Guidelines
- **Input validation** for all user inputs
- **No hardcoded secrets** in code
- **Secure defaults** for configurations
- **Proper error handling** without information leakage
- **Security tests** for all security features

## Testing Guidelines

### Test Categories
1. **Unit Tests** - Test individual components
2. **Integration Tests** - Test component interactions
3. **Security Tests** - Test security features
4. **Performance Tests** - Test virtual threads performance

### Test Requirements
- **100% coverage** for new features
- **Security tests** for security-related changes
- **Performance tests** for performance-related changes
- **Integration tests** for API changes

### Test Example:
```kotlin
@Test
@DisplayName("Should authenticate user with valid JWT token")
fun testJWTAuthentication() {
    // Given
    val jwtService = JWTService(testConfig)
    val token = jwtService.createToken("testuser", setOf("USER"))
    
    // When
    val result = jwtService.validateToken(token.token)
    
    // Then
    assertTrue(result is JWTValidationResult.Valid)
    assertEquals("testuser", (result as JWTValidationResult.Valid).token.claims.subject)
}
```

## Documentation

### Documentation Requirements
- **Update README.md** for new features
- **Add examples** for new functionality
- **Update CHANGELOG.md** for all changes
- **Add KDoc comments** for public APIs
- **Update configuration docs** for new options

### Documentation Style
- Clear and concise explanations
- Practical examples
- Step-by-step instructions
- Security considerations
- Performance implications

## Development Workflow

### Branch Naming
- `feature/feature-name` - New features
- `bugfix/issue-description` - Bug fixes
- `security/security-improvement` - Security enhancements
- `docs/documentation-update` - Documentation changes

### Commit Messages
Follow conventional commits format:
```
type(scope): description

- Detailed explanation
- Multiple points if needed
- Reference issues: fixes #123
```

Types: `feat`, `fix`, `docs`, `style`, `refactor`, `test`, `chore`, `security`

### Release Process
1. Update version in `build.gradle.kts`
2. Update `CHANGELOG.md`
3. Create release PR
4. Tag release after merge
5. GitHub Actions will handle publishing

## Community

### Communication Channels
- **GitHub Issues** - Bug reports, feature requests
- **GitHub Discussions** - General discussions, questions
- **Pull Requests** - Code review and collaboration

### Recognition
Contributors will be:
- Added to contributors list
- Mentioned in release notes
- Credited in documentation

## Getting Help

If you need help:
1. Check existing documentation
2. Search existing issues
3. Ask in [GitHub Discussions](../../discussions)
4. Create a question issue with our template

Thank you for contributing to WebFramework! 🚀