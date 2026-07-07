# Gradle proxy and repository troubleshooting

Last verified: 2026-05-15

Use this document when Gradle wrapper bootstrap or Android/Gradle repository
resolution fails behind a proxy.

Common symptoms:

- `Unable to tunnel through proxy. Proxy returns "HTTP/1.1 403 Forbidden"`
- plugin resolution failures from `gradlePluginPortal()`, `google()`, or
  `mavenCentral()`

## Quick Diagnostics

```bash
# Project-level automated check
./scripts/project-check.sh

# Validate wrapper script can run
./gradlew --version

# Verify proxy-related environment variables
env | grep -Ei '(^|_)(http|https|no)_proxy|GRADLE_OPTS|GRADLE_REPO_MIRROR_URL|GRADLE_WRAPPER_DIST_URL'

# Check direct Gradle distribution reachability
curl -I https://services.gradle.org/distributions/gradle-9.3.1-bin.zip
```

`scripts/project-check.sh` also checks Android/Gradle plugin repositories
(`dl.google.com`, Maven Central, Gradle Plugin Portal) and, when available, a
system Gradle repository-resolution probe.

## Explicit Gradle Proxy

Set JVM proxy flags when running Gradle:

```bash
export GRADLE_OPTS="-Dhttps.proxyHost=<proxy-host> -Dhttps.proxyPort=<proxy-port> -Dhttp.proxyHost=<proxy-host> -Dhttp.proxyPort=<proxy-port>"
./gradlew test
```

If the proxy needs authentication:

```bash
export GRADLE_OPTS="$GRADLE_OPTS -Dhttps.proxyUser=<user> -Dhttps.proxyPassword=<password>"
```

The same values can be stored in `~/.gradle/gradle.properties` as
`systemProp.*` entries.

## Internal Maven Mirror

This project supports an optional mirror repository URL for both plugin
resolution and normal dependencies.

Use either:

- environment variable: `GRADLE_REPO_MIRROR_URL`
- Gradle property: `-PrepoMirrorUrl=<url>`

Example:

```bash
export GRADLE_REPO_MIRROR_URL=https://nexus.example.com/repository/maven-public/
./gradlew test
```

If public artifact hosts are blocked, force mirror-only mode:

```bash
export GRADLE_REPO_MIRROR_URL=https://nexus.example.com/repository/maven-public/
export GRADLE_REPO_MIRROR_ONLY=true
./gradlew test
```

Equivalent Gradle properties:

```bash
./gradlew test -PrepoMirrorUrl=https://nexus.example.com/repository/maven-public/ -PrepoMirrorOnly=true
```

## Wrapper Distribution Mirror

`repoMirrorUrl` helps plugin and dependency resolution, but not the Gradle
wrapper distribution ZIP. If the proxy blocks Gradle distribution hosts, mirror
the ZIP internally and set one of:

```properties
distributionUrl=https://<internal-host>/gradle-distributions/gradle-9.3.1-bin.zip
```

```bash
export GRADLE_WRAPPER_DIST_URL=https://<internal-host>/gradle-distributions/gradle-9.3.1-bin.zip
./scripts/use-wrapper-dist-mirror.sh "$GRADLE_WRAPPER_DIST_URL"
```

Keep the same `distributionSha256Sum` if the mirror serves the unmodified
official ZIP.

## Offline Fallback

If wrapper distribution download is blocked, pre-seed
`~/.gradle/wrapper/dists` from a machine with internet access and then rerun:

```bash
./gradlew --offline test
```

Offline mode still requires all plugins and dependencies to exist in the local
cache or internal mirror.

## Validation

- Run `./scripts/project-check.sh` after any proxy, mirror, or wrapper
  distribution change.
- Expected good state: wrapper bootstrap succeeds and repository resolution is
  reachable from either public hosts or the configured internal mirror.
- If only wrapper bootstrap fails, validate `GRADLE_WRAPPER_DIST_URL` and
  `distributionUrl`.
- If repository resolution fails, validate `GRADLE_REPO_MIRROR_URL` and
  `GRADLE_REPO_MIRROR_ONLY`.
