#!/usr/bin/env bash
set -euo pipefail

echo "== SimonApp project check =="
echo

echo "-- Proxy-related environment --"
env | rg -i '(^|_)(http|https|no)_proxy|GRADLE_OPTS|GRADLE_REPO_MIRROR_URL|GRADLE_WRAPPER_DIST_URL' || true
echo

echo "-- Gradle wrapper executable bit --"
if [[ -x "./gradlew" ]]; then
  echo "OK: ./gradlew is executable"
else
  echo "WARN: ./gradlew is not executable (run: chmod +x ./gradlew)"
fi
echo

check_url() {
  local url="$1"
  printf "Checking %s ... " "$url"
  if curl -I -sS --max-time 20 "$url" >/dev/null; then
    echo "OK"
  else
    echo "FAILED"
  fi
}

echo "-- Reachability checks via current network/proxy --"
check_url "https://services.gradle.org/distributions/gradle-9.3.1-bin.zip"
check_url "https://downloads.gradle.org/distributions/gradle-9.3.1-bin.zip"
check_url "https://dl.google.com/dl/android/maven2/"
check_url "https://repo.maven.apache.org/maven2/"
check_url "https://plugins.gradle.org/m2/"
if [[ -n "${GRADLE_WRAPPER_DIST_URL:-}" ]]; then
  check_url "$GRADLE_WRAPPER_DIST_URL"
fi
echo

echo "-- Gradle wrapper bootstrap check --"
if ./gradlew --version; then
  echo "OK: wrapper bootstrap succeeded"
else
  echo "FAILED: wrapper bootstrap failed"
  cat <<'MSG'
Hint:
  - if proxy blocks gradle distribution hosts, use an internal mirror for the wrapper ZIP
  - set GRADLE_WRAPPER_DIST_URL and update gradle/wrapper/gradle-wrapper.properties distributionUrl accordingly
  - set GRADLE_REPO_MIRROR_URL for plugin/dependency resolution
MSG
fi
echo

echo "-- Buildscript repository resolution check (system Gradle) --"
if command -v gradle >/dev/null 2>&1; then
  if gradle -q help --no-daemon >/dev/null; then
    echo "OK: buildscript repositories are reachable"
  else
    echo "FAILED: buildscript repositories are not reachable"
    cat <<'MSG'
Hint:
  - verify proxy allows: dl.google.com, repo.maven.apache.org, plugins.gradle.org
  - if public egress is denied, configure GRADLE_REPO_MIRROR_URL to an internal Maven mirror
MSG
  fi
else
  echo "WARN: system gradle command not found; skipped repository resolution check"
fi
