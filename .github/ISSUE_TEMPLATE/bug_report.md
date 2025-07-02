---
name: Bug Report
about: Create a report to help us improve
title: '[BUG] '
labels: 'bug'
assignees: ''
---

## Bug Description
A clear and concise description of what the bug is.

## To Reproduce
Steps to reproduce the behavior:
1. Set up WebFramework with '...'
2. Configure security with '...'
3. Make request to '...'
4. See error

## Expected Behavior
A clear and concise description of what you expected to happen.

## Actual Behavior
What actually happened instead.

## Environment
- **WebFramework Version**: [e.g., 1.0.0]
- **JDK Version**: [e.g., JDK 21]
- **Spring Boot Version**: [e.g., 3.2.1] (if applicable)
- **OS**: [e.g., Ubuntu 22.04, macOS 14.0, Windows 11]
- **Gradle/Maven Version**: [e.g., Gradle 8.0]

## Code Example
```kotlin
// Minimal code example that reproduces the issue
val framework = WebFramework.create()
framework.get("/test") { ctx ->
    // Your code here
}
```

## Logs/Stack Trace
```
// Paste relevant logs or stack traces here
```

## Additional Context
Add any other context about the problem here, such as:
- Configuration files
- Related issues
- Potential workarounds

## Checklist
- [ ] I have searched existing issues to ensure this is not a duplicate
- [ ] I have provided a minimal code example
- [ ] I have included relevant logs/stack traces
- [ ] I am using a supported JDK version (21+)