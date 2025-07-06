#!/bin/bash

# F.O.R.G.E v1.0.0 Release Creation Script
# Run this script after completing GitHub authentication

set -e

echo "🚀 Creating F.O.R.G.E v1.0.0 Release..."

# Check if GitHub CLI is authenticated
if ! gh auth status > /dev/null 2>&1; then
    echo "❌ GitHub CLI is not authenticated"
    echo "Please run: gh auth login -h github.com"
    echo "Use the device code: 0D4A-93A2 (if still valid)"
    echo "URL: https://github.com/login/device"
    exit 1
fi

echo "✅ GitHub CLI authenticated"

# Check if release already exists
if gh release view v1.0.0 > /dev/null 2>&1; then
    echo "⚠️  Release v1.0.0 already exists"
    read -p "Do you want to delete and recreate it? (y/N): " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        echo "🗑️  Deleting existing release..."
        gh release delete v1.0.0 --yes
    else
        echo "❌ Aborting release creation"
        exit 1
    fi
fi

# Ensure we have the latest changes
echo "📥 Pulling latest changes..."
git pull origin main

# Build the artifacts
echo "🔨 Building release artifacts..."
export JAVA_HOME=/home/lfneves/.sdkman/candidates/java/21.0.1-tem
export PATH=$JAVA_HOME/bin:$PATH
./gradlew clean build -x test

# Check if artifacts exist
ARTIFACTS=(
    "build/libs/forge-1.0.0.jar"
    "build/libs/forge-1.0.0-plain.jar"
    "build/libs/forge-1.0.0-sources.jar"
    "build/libs/forge-1.0.0-javadoc.jar"
)

echo "📦 Checking release artifacts..."
for artifact in "${ARTIFACTS[@]}"; do
    if [ ! -f "$artifact" ]; then
        echo "❌ Missing artifact: $artifact"
        exit 1
    else
        echo "✅ Found: $artifact ($(du -h "$artifact" | cut -f1))"
    fi
done

# Create the release
echo "🎉 Creating GitHub release v1.0.0..."
gh release create v1.0.0 \
  --title "F.O.R.G.E v1.0.0 - Framework Optimized for Resilient, Global Execution" \
  --notes-file RELEASE_NOTES_v1.0.0.md \
  "${ARTIFACTS[@]}"

echo ""
echo "🎊 Release v1.0.0 created successfully!"
echo ""
echo "📋 Release includes:"
echo "   • forge-1.0.0.jar (Complete Spring Boot executable)"
echo "   • forge-1.0.0-plain.jar (Core framework library)"
echo "   • forge-1.0.0-sources.jar (Source code archive)"
echo "   • forge-1.0.0-javadoc.jar (API documentation)"
echo ""
echo "🔗 View release: https://github.com/lfneves/F.O.R.G.E/releases/tag/v1.0.0"
echo ""
echo "✨ F.O.R.G.E v1.0.0 is now live and ready for production!"