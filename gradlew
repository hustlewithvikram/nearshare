#!/usr/bin/env sh
# Lightweight Gradle launcher for this repository. It downloads and caches the
# pinned Gradle distribution, then forwards all arguments to Gradle.
set -eu
GRADLE_VERSION="8.13"
GRADLE_HOME="${GRADLE_USER_HOME:-$HOME/.gradle}/nearshare-gradle/gradle-$GRADLE_VERSION"
GRADLE_BIN="$GRADLE_HOME/bin/gradle"
if [ ! -x "$GRADLE_BIN" ]; then
  BASE="${GRADLE_USER_HOME:-$HOME/.gradle}/nearshare-gradle"
  ZIP="$BASE/gradle-$GRADLE_VERSION-bin.zip"
  mkdir -p "$BASE"
  if [ ! -f "$ZIP" ]; then
    if command -v curl >/dev/null 2>&1; then
      curl -fL --retry 3 "https://services.gradle.org/distributions/gradle-$GRADLE_VERSION-bin.zip" -o "$ZIP"
    elif command -v wget >/dev/null 2>&1; then
      wget "https://services.gradle.org/distributions/gradle-$GRADLE_VERSION-bin.zip" -O "$ZIP"
    else
      echo "curl or wget is required to download Gradle." >&2
      exit 1
    fi
  fi
  command -v unzip >/dev/null 2>&1 || { echo "unzip is required to install Gradle." >&2; exit 1; }
  unzip -q -o "$ZIP" -d "$BASE"
fi
exec "$GRADLE_BIN" "$@"
