# Project check report (2026-03-27)

Last verified: 2026-05-15

## Scope

- Basic repository sanity checks
- Proxy-related network reachability checks
- Gradle wrapper/bootstrap and buildscript repository resolution checks

## Commands Executed

```bash
./scripts/project-check.sh
bash ./scripts/project-check.sh
```

## Findings

1. `scripts/project-check.sh` was not executable at first, then ran
   successfully after the executable bit was fixed.
2. Proxy variables were present and consistently configured:
   `HTTP_PROXY`, `HTTPS_PROXY`, `http_proxy`, `https_proxy`, and `GRADLE_OPTS`.
3. External Gradle/Android Maven endpoints were blocked through the current
   proxy with `CONNECT tunnel failed, response 403`:
   `services.gradle.org`, `downloads.gradle.org`, `dl.google.com`,
   `repo.maven.apache.org`, and `plugins.gradle.org`.
4. `./gradlew --version` succeeded in that environment, indicating wrapper
   bootstrap can work when distribution artifacts are already available locally
   or in cache.
5. Buildscript plugin resolution still failed because the configured
   repositories were not reachable through current proxy policy.

## Current Status

- Project bootstrap check: partially OK when the wrapper distribution is
  available locally or via an allowed mirror.
- Dependency/plugin resolution: blocked until public repository hosts are
  allow-listed or an internal Maven mirror is configured.
- Detailed remediation lives in `GRADLE_PROXY_TROUBLESHOOTING.md`.

## Validation

- Re-run `./scripts/project-check.sh` after proxy allow-list, mirror, wrapper
  distribution, or Gradle repository changes.
- Expected good state: wrapper bootstrap succeeds and buildscript repository
  resolution succeeds.
- If the same 403 failures return, follow `GRADLE_PROXY_TROUBLESHOOTING.md`
  rather than duplicating remediation steps in this dated report.
