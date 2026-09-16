# Java engine layout (`:app` / `com.hsw.simonapp.engine`)

Last verified: 2026-07-07

The app module owns the embeddable Java engine. Android host code uses only the
public `com.hsw.simonapp.engine.api` facade; implementation packages under
`com.hsw.simonapp.engine` outside `api` are internal to the engine package
tree, except the `game` definition surface used to configure app-specific game
content.

## Ownership

| Area | Package | Notes |
| --- | --- | --- |
| Host-facing API | `api/` | `EmbeddedEngine`, touch payloads, immutable scene payloads, and the narrow native facade. |
| Game definition surface | `game/` | `GameDefinition`, `SceneDefinition`, and `GameRuntimeFactory` describe app-specific scene assets, initial world state, and loop wiring without touching renderer internals. |
| Render loop | `render/` | Java `Choreographer` frame producer, render consumer thread, latest-frame mailbox, renderer lifecycle, Vulkan session coordination, and scene-frame submission. |
| Scene/entity model | `loop/*`, `scene/*` | Deterministic update loop, world state, collision, and snapshot conversion. |
| Assets | `assets/`, `scene/TextureCatalog` | Texture loading, generated shape textures, atlas metadata, and default scene catalog. |
| Audio API/backend | `audio/` | Event queue, throttling, and Android-backed playback implementation. |
| JNI plumbing | `:VulkanRenderingEngine` / `com.hsw.vulkanrenderingengine.bridge` | JNI bridge contract, adapter, and raw native declarations are packaged with the reusable renderer AAR and hidden behind `api.NativeLib` in this app. |
| Frame diagnostics | `render/FrameDiagnostics` | Logs `EngineFrameTiming` summaries, slow/catch-up frames, update/audio/snapshot/native timings, and mailbox overwrite counts. |

## Current vs Target

| Area | Current | Target |
| --- | --- | --- |
| Public API | `api/` contains the host-facing embedding contract and immutable payload types. | Keep host apps isolated from engine internals and JNI declarations. |
| Game configuration | `DefaultGameDefinition` preserves the current sample game while `EmbeddedEngine` can accept another `GameDefinition`. | Move app-specific assets, scene content, and runtime wiring behind definitions instead of hard-coding them in render runtime classes. |
| Engine internals | `assets/`, `audio/`, `input/`, `loop/`, `render/`, and `scene/` own update, render-loop coordination, scene conversion, assets, input queues, and audio events. | Keep authoritative world mutation and event decisions in Java engine code. |
| Bridge layer | `com.hsw.vulkanrenderingengine.bridge` adapts Java engine calls to native JNI functions from the renderer AAR. | Keep raw native declarations hidden behind `api.NativeLib` and bridge adapters. |
| Native payloads | Render state crosses to C++ as immutable `SceneFrame`/snapshot data. | Preserve immutable snapshot handoff and avoid native ownership of world mutation. |
| Frame pacing | `RenderLoopController` uses Android `Choreographer` on a producer thread and a latest-frame mailbox feeding a render consumer thread. | Keep one primary frame pacer active; native Swappy pacing remains disabled unless it is deliberately reintroduced and retested. |

## Boundary Rules

- Host code outside `com.hsw.simonapp.engine` uses `api` for embedding and
  never calls the renderer bridge directly.
- A host that supplies custom game content may additionally provide a
  `game.GameDefinition`; it still must not import `render`, JNI bridge, or
  native backend packages directly.
- Engine implementation packages may depend on `api` and Android framework types needed for
  embedding, but app code must not depend on `engine` packages.
- Render state crosses the JNI/Vulkan boundary as immutable `SceneFrame`
  payloads.
- Input crosses from host to engine as public `TouchInputEvent` payloads.
- Audio playback is triggered by engine update events, not by host UI callbacks.

## Public Embedding Contract

`EmbeddedEngine` is the object a host embeds. It accepts host-owned lifecycle
signals (`initialize`, `resize`, `start`, `stop`, `stopAndRelease`) and input
payloads (`queueTouchInput`). A host may use the default game or pass a
`GameDefinition` during construction. Everything after that point is
engine-owned.

## Runtime notes

- The Java engine currently treats the renderer as a singleton backend through
  `api.NativeLib` and the AAR bridge. This supports reuse from different apps,
  but not multiple simultaneous renderer instances inside one process.
- `SceneDefinition` supplies texture registrations and initial world state.
- `GameRuntimeFactory` supplies the `GameLoop`, viewport adapter, and
  interaction adapter used by `EngineGameLoopRuntime`.
- Frame cadence is driven by Java `Choreographer`; the native renderer should
  perform one submitted render frame and return without owning gameplay timing.
- `FrameDiagnostics` is diagnostic-only. Its catch-up and slow-frame warnings
  describe timing behavior and do not change gameplay or rendering policy.

## Validation

- Host import boundary: host Java sources must not import engine implementation
  packages outside `com.hsw.simonapp.engine.api.*`; custom game bootstraps may
  also depend on `com.hsw.simonapp.engine.game.*`.
- Engine package check: `api/` and implementation package roots must remain separate
  package roots under `com.hsw.simonapp.engine`; reusable game definitions
  live under `game/`; JNI bridge classes live under
  `com.hsw.vulkanrenderingengine.bridge` in `:VulkanRenderingEngine`.
- Frame pacing check: `RenderLoopController` remains the app engine's active
  `Choreographer`-based scheduler; native Swappy queue-present pacing is not
  the active runtime path.
- Build check, when Gradle dependencies are cached or reachable:
  `./gradlew :app:assembleDebug` including its `:VulkanRenderingEngine` dependency.
- Project bootstrap check: `./scripts/project-check.sh`.
