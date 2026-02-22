#!/bin/bash
# Gradle wrapper script
# Adapted from standard Gradle wrapper

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
GRADLE_WRAPPER_JAR="$SCRIPT_DIR/gradle/wrapper/gradle-wrapper.jar"
GRADLE_WRAPPER_PROPERTIES="$SCRIPT_DIR/gradle/wrapper/gradle-wrapper.properties"

if [ ! -f "$GRADLE_WRAPPER_JAR" ]; then
    echo "Gradle wrapper jar not found at $GRADLE_WRAPPER_JAR"
    exit 1
fi

# Parse properties (simple format)
LINE=$(grep -E "^distributionUrl=" "$GRADLE_WRAPPER_PROPERTIES")
DIST_URL="${LINE#distributionUrl=}"
# Unescape \: to : and \= to =
DIST_URL="${DIST_URL//\\:/:}"
DIST_URL="${DIST_URL//\\=/=}"

# Extract version
if [[ "$DIST_URL" =~ gradle-([0-9.]+)-bin ]]; then
    GRADLE_VERSION="${BASH_REMATCH[1]}"
else
    echo "Could not determine Gradle version from URL: $DIST_URL"
    exit 1
fi

# Determine Gradle home path (wrapper cache)
GRADLE_HOME="${HOME}/.gradle/wrapper/dists/gradle-${GRADLE_VERSION}-bin"

# Download if not cached
if [ ! -d "$GRADLE_HOME" ]; then
    echo "Downloading Gradle ${GRADLE_VERSION}..."
    mkdir -p "$GRADLE_HOME"
    TMP_ZIP="/tmp/gradle-${GRADLE_VERSION}-bin.zip"
    curl -fSL "$DIST_URL" -o "$TMP_ZIP"
    unzip -q "$TMP_ZIP" -d "$GRADLE_HOME"
    rm "$TMP_ZIP"
fi

# Find the actual unpacked Gradle directory (it contains a subfolder with a hash)
GRADLE_BIN_DIR=$(find "$GRADLE_HOME" -name "gradle-${GRADLE_VERSION}" -type d | head -n1)
if [ -z "$GRADLE_BIN_DIR" ]; then
    # Some versions may have a different naming pattern, find any bin/gradle
    GRADLE_BIN_DIR=$(find "$GRADLE_HOME" -name "bin" -type d | head -n1)
    if [ -n "$GRADLE_BIN_DIR" ]; then
        GRADLE_BIN_DIR="$(dirname "$GRADLE_BIN_DIR")"
    fi
fi

if [ -z "$GRADLE_BIN_DIR" ] || [ ! -x "$GRADLE_BIN_DIR/bin/gradle" ]; then
    echo "Gradle installation not found in $GRADLE_HOME"
    exit 1
fi

# Run Gradle
exec "$GRADLE_BIN_DIR/bin/gradle" "$@"
