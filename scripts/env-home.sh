#!/usr/bin/env bash
set -euo pipefail

# Home profile: disable all proxy-related settings for this shell.
unset HTTP_PROXY HTTPS_PROXY http_proxy https_proxy ALL_PROXY all_proxy
unset GRADLE_OPTS
unset GRADLE_REPO_MIRROR_URL

echo "[env-home] Proxy kikapcsolva ehhez a shellhez."
echo "[env-home] Ellenőrzés: env | rg -i 'proxy|gradle'"
