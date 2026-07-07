#!/usr/bin/env bash
set -euo pipefail

# Work profile: enable corporate proxy settings.
# Igazítsd a host/port értékeket a saját munkahelyi környezetedhez.
PROXY_HOST="${PROXY_HOST:-proxy}"
PROXY_PORT="${PROXY_PORT:-8080}"
PROXY_URL="http://${PROXY_HOST}:${PROXY_PORT}"

export HTTP_PROXY="${HTTP_PROXY:-$PROXY_URL}"
export HTTPS_PROXY="${HTTPS_PROXY:-$PROXY_URL}"
export http_proxy="$HTTP_PROXY"
export https_proxy="$HTTPS_PROXY"

# Gradle JVM proxy flags
export GRADLE_OPTS="-Dhttp.proxyHost=${PROXY_HOST} -Dhttp.proxyPort=${PROXY_PORT} -Dhttps.proxyHost=${PROXY_HOST} -Dhttps.proxyPort=${PROXY_PORT}"

# Optional: set your internal mirror URL here or externally before sourcing this script.
# export GRADLE_REPO_MIRROR_URL="https://nexus.example.com/repository/maven-public/"
# export GRADLE_REPO_MIRROR_ONLY="true"
# Optional: internal mirror for Gradle wrapper distribution ZIP.
# export GRADLE_WRAPPER_DIST_URL="https://nexus.example.com/repository/gradle-distributions/gradle-9.3.1-bin.zip"

if [[ -n "${GRADLE_REPO_MIRROR_URL:-}" ]]; then
  echo "[env-work] Mirror: $GRADLE_REPO_MIRROR_URL"
  echo "[env-work] Mirror only mód: ${GRADLE_REPO_MIRROR_ONLY:-false}"
else
  echo "[env-work] Mirror nincs beállítva (GRADLE_REPO_MIRROR_URL)."
fi

if [[ -n "${GRADLE_WRAPPER_DIST_URL:-}" ]]; then
  echo "[env-work] Wrapper dist URL: $GRADLE_WRAPPER_DIST_URL"
else
  echo "[env-work] Wrapper dist URL nincs beállítva (GRADLE_WRAPPER_DIST_URL)."
fi

echo "[env-work] Proxy bekapcsolva ehhez a shellhez: $PROXY_URL"
echo "[env-work] Ellenőrzés: env | rg -i 'proxy|gradle'"
