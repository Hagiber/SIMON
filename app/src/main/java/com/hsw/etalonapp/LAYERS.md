# Host module layout (`:app`)

Last verified: 2026-07-07

This package owns the Android shell around the embedded Java engine. Host code
uses `com.hsw.etalonapp.engine.api.EmbeddedEngine`. Hosts that provide custom
game content may also pass a `com.hsw.etalonapp.engine.game.GameDefinition`,
but must not import engine implementation packages such as
`com.hsw.etalonapp.engine.render.*` or the renderer bridge package
`com.hsw.vulkanrenderingengine.bridge.*`.

## Ownership

| Area | Owner | Notes |
| --- | --- | --- |
| Activity and lifecycle | `:app` | Creates views, observes `SurfaceHolder`, and decides when the embedded engine may start or stop. |
| Input events | `:app` | Receives Android `MotionEvent`s and translates them to public `TouchInputEvent` payloads. |
| Permissions | `:app` | Runtime permission prompts and Android policy remain outside the engine. |
| UI/debug overlay | `:app` | Status text, selected-object controls, and coordinate display stay host-side. |
| Render loop | `com.hsw.etalonapp.engine.render` | Host never calls frame rendering or owns render timing. |
| Native render backend | `:VulkanRenderingEngine` | Vulkan C++ backend, shader assets, and the reusable Java bridge stay outside the host package. |
| Scene/entity/assets/audio | `com.hsw.etalonapp.engine.*` | Host passes `AssetManager` and user input only. |

## Current vs Target

| Area | Current | Target |
| --- | --- | --- |
| Host surface | `MainActivity`, `input/`, and `ui/` contain the active host code. | Keep host code limited to Android embedding, lifecycle, input adaptation, and UI state. |
| Empty or legacy package folders | Some older package folders may still exist without active Java sources. | Do not reintroduce world, render-loop, audio, or asset ownership into `:app`. |
| Engine access | Host talks to the embedded engine through `com.hsw.etalonapp.engine.api.*`. | Preserve `api` as the only supported embedding surface. |
| Game configuration | The default app uses the built-in `DefaultGameDefinition`, including its save/load persistence. | Other apps may provide a `GameDefinition` while still keeping render/runtime ownership inside the engine. |
| Renderer dependency | The app module depends on `:VulkanRenderingEngine`, but host Java code does not import its bridge package directly. | Keep renderer access routed through the embedded engine API so another host can replace only the app shell. |

## Host Package Intent

- `MainActivity`: Android embedding point for the engine.
- `input/`: Android input adapter only; no queue draining or gameplay ownership.
- `ui/`: lifecycle state helpers and host-only UI state.

## Boundary Rules

- Host code may import `com.hsw.etalonapp.engine.api.*`.
- Custom game bootstrap code may additionally import
  `com.hsw.etalonapp.engine.game.*` to provide a `GameDefinition`.
- Host code must not import engine implementation packages such as
  `com.hsw.etalonapp.engine.render.*` or renderer bridge package
  `com.hsw.vulkanrenderingengine.bridge.*`.
- App code must not own render-loop threads, Vulkan sessions, scene snapshots,
  world state, asset upload logic, or audio playback policy.
- UI callbacks may call public engine commands such as `queueTouchInput(...)`
  and `reverseSelectedEntityDirection()`.

## Validation

- Import boundary check: host Java sources must have no matches for
  engine implementation package imports outside `com.hsw.etalonapp.engine.api.*`.
- Unit boundary check:
  `HostModuleBoundaryTest.hostSourcesDoNotImportEngineInternalsOrRendererBridge`
  rejects host imports of engine internals and
  `com.hsw.vulkanrenderingengine.bridge.*`.
- Build check, when Gradle dependencies are cached or reachable:
  `./gradlew :app:assembleDebug`.
- Project bootstrap check: `./scripts/project-check.sh`.
