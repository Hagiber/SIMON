# Architecture documentation audit (2026-05-06)

Last verified: 2026-06-30

## Status

This audit is retained as a short historical checkpoint. The actionable items
from the original audit have been folded into the active layer documents:

- `app/src/main/java/com/hsw/etalonapp/LAYERS.md`
- `app/src/main/java/com/hsw/etalonapp/engine/LAYERS.md`
- `VulkanRenderingEngine/src/main/cpp/LAYERS.md`

## Resolved Cleanup

- Added a consistent `Last verified: YYYY-MM-DD` marker to each tracked
  Markdown document.
- Split layer docs into current responsibilities, target boundaries, and
  explicit validation steps.
- Removed repeated architecture rules by keeping the host/API boundary in the
  Java layer docs and native renderer ownership in the C++ layer doc.
- Kept the current reusable renderer boundary, singleton native facade, sample
  host role, and Java `Choreographer` frame pacing details in the active layer
  docs instead of this historical audit.
- Kept project bootstrap and proxy guidance in `GRADLE_PROXY_TROUBLESHOOTING.md`
  instead of repeating it in every dated project-check report.

## Remaining Watchpoints

- Re-check layer docs whenever package roots, JNI entry points, renderer
  lifecycle, frame pacing, input handoff, audio policy, or Gradle/proxy
  bootstrap behavior changes.
- Keep dated project-check reports concise. Current remediation steps belong in
  the troubleshooting guide.

## Validation

- Every tracked Markdown document should contain a `Last verified: YYYY-MM-DD`
  marker and a `## Validation` section.
- Host Java sources outside `com.hsw.etalonapp.engine` should only import the
  public `com.hsw.etalonapp.engine.api.*` facade.
- Run `./scripts/project-check.sh` before release checkpoints or after
  Gradle/proxy configuration changes.
