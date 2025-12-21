#!/bin/bash

# Build release script for UltimateInventory Client Mod
# Usage: ./build-release.sh [patch|minor|major]
# Default: patch version increment

set -e  # Exit on error

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

GRADLE_PROPERTIES="gradle.properties"
RELEASES_DIR="releases"

# Get increment type (default: patch)
INCREMENT_TYPE="${1:-patch}"

# Validate increment type
if [[ ! "$INCREMENT_TYPE" =~ ^(patch|minor|major)$ ]]; then
    echo "Error: Invalid increment type '$INCREMENT_TYPE'"
    echo "Usage: $0 [patch|minor|major]"
    exit 1
fi

# Read current version from gradle.properties
if [ ! -f "$GRADLE_PROPERTIES" ]; then
    echo "Error: $GRADLE_PROPERTIES not found!"
    exit 1
fi

CURRENT_VERSION=$(grep "^mod_version=" "$GRADLE_PROPERTIES" | cut -d'=' -f2)
MINECRAFT_VERSION=$(grep "^minecraft_version=" "$GRADLE_PROPERTIES" | cut -d'=' -f2)

if [ -z "$CURRENT_VERSION" ]; then
    echo "Error: Could not find mod_version in $GRADLE_PROPERTIES"
    exit 1
fi

if [ -z "$MINECRAFT_VERSION" ]; then
    echo "Error: Could not find minecraft_version in $GRADLE_PROPERTIES"
    exit 1
fi

echo "Current version: $CURRENT_VERSION"
echo "Minecraft version: $MINECRAFT_VERSION"
echo "Increment type: $INCREMENT_TYPE"

# Parse version (assumes semantic versioning: X.Y.Z)
IFS='.' read -ra VERSION_PARTS <<< "$CURRENT_VERSION"
MAJOR="${VERSION_PARTS[0]:-0}"
MINOR="${VERSION_PARTS[1]:-0}"
PATCH="${VERSION_PARTS[2]:-0}"

# Increment version based on type
case "$INCREMENT_TYPE" in
    major)
        MAJOR=$((MAJOR + 1))
        MINOR=0
        PATCH=0
        ;;
    minor)
        MINOR=$((MINOR + 1))
        PATCH=0
        ;;
    patch)
        PATCH=$((PATCH + 1))
        ;;
esac

NEW_VERSION="$MAJOR.$MINOR.$PATCH"
echo "New version: $NEW_VERSION"

# Update version in gradle.properties
if [[ "$OSTYPE" == "darwin"* ]]; then
    # macOS
    sed -i '' "s/^mod_version=.*/mod_version=$NEW_VERSION/" "$GRADLE_PROPERTIES"
else
    # Linux
    sed -i "s/^mod_version=.*/mod_version=$NEW_VERSION/" "$GRADLE_PROPERTIES"
fi

echo "Updated $GRADLE_PROPERTIES with new version: $NEW_VERSION"

# Clean and build
echo "Building mod..."
./gradlew clean build --quiet

# Ensure releases directory exists
mkdir -p "$RELEASES_DIR"

# Remove old JARs for the same Minecraft version (but keep sources if user wants them)
echo "Removing old releases for Minecraft $MINECRAFT_VERSION..."
find "$RELEASES_DIR" -name "*${MINECRAFT_VERSION}-*.jar" -type f ! -name "*-sources.jar" -delete

# Copy new JAR to releases (excluding sources JAR)
NEW_JAR=$(find build/libs -name "*-${MINECRAFT_VERSION}-*.jar" -type f ! -name "*-sources.jar" | head -1)

if [ -z "$NEW_JAR" ]; then
    echo "Error: Could not find built JAR file!"
    exit 1
fi

echo "Copying $NEW_JAR to $RELEASES_DIR/"
cp "$NEW_JAR" "$RELEASES_DIR/"

echo ""
echo "✓ Build successful!"
echo "  Version: $NEW_VERSION"
echo "  Minecraft: $MINECRAFT_VERSION"
echo "  Release file: $RELEASES_DIR/$(basename "$NEW_JAR")"
echo ""
echo "To commit the version change:"
echo "  git add $GRADLE_PROPERTIES $RELEASES_DIR/"
echo "  git commit -m \"Bump version to $NEW_VERSION for MC $MINECRAFT_VERSION\""

