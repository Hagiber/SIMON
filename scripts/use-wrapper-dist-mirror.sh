#!/usr/bin/env bash
set -euo pipefail

MIRROR_URL="${1:-${GRADLE_WRAPPER_DIST_URL:-}}"

if [[ -z "$MIRROR_URL" ]]; then
  echo "Usage: $0 <mirror-distribution-url>"
  echo "or set GRADLE_WRAPPER_DIST_URL and run: $0"
  exit 1
fi

TARGET_FILE="gradle/wrapper/gradle-wrapper.properties"
if [[ ! -f "$TARGET_FILE" ]]; then
  echo "ERROR: $TARGET_FILE not found"
  exit 1
fi

escaped_url="${MIRROR_URL//\//\\/}"
escaped_url="${escaped_url//:/\\:}"

sed -i -E "s#^distributionUrl=.*#distributionUrl=${escaped_url}#" "$TARGET_FILE"
echo "Updated distributionUrl in $TARGET_FILE"
echo "New value:"
rg '^distributionUrl=' "$TARGET_FILE"
