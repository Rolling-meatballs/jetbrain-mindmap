#!/usr/bin/env bash
set -euo pipefail

GRADLE_VERSION="${1:-8.13}"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
TMP_DIR="${TMPDIR:-/tmp}/mindmap-gradle-bootstrap"
ZIP_FILE="$TMP_DIR/gradle-${GRADLE_VERSION}-bin.zip"
DIST_DIR="$TMP_DIR/gradle-${GRADLE_VERSION}"

mkdir -p "$TMP_DIR"

is_valid_zip() {
  local file="$1"
  [[ -f "$file" ]] || return 1
  unzip -tq "$file" >/dev/null 2>&1
}

download_zip() {
  local tmp_zip="${ZIP_FILE}.partial"
  echo "[bootstrap] Downloading Gradle $GRADLE_VERSION"
  rm -f "$tmp_zip"
  curl --fail --location --retry 3 --retry-delay 1 \
    "https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip" \
    -o "$tmp_zip"
  mv "$tmp_zip" "$ZIP_FILE"
}

if ! is_valid_zip "$ZIP_FILE"; then
  echo "[bootstrap] Cached archive is missing or invalid, refreshing"
  rm -f "$ZIP_FILE"
  download_zip
fi

if [[ ! -d "$DIST_DIR" ]]; then
  echo "[bootstrap] Unpacking Gradle $GRADLE_VERSION"
  unzip -q "$ZIP_FILE" -d "$TMP_DIR"
fi

echo "[bootstrap] Generating Gradle wrapper in $PROJECT_DIR"
"$DIST_DIR/bin/gradle" -p "$PROJECT_DIR" wrapper --gradle-version "$GRADLE_VERSION" --distribution-type bin

echo "[bootstrap] Done. Use: ./gradlew runIde"
