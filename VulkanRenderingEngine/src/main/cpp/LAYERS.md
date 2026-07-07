# Native layer layout (`:VulkanRenderingEngine`)

Last verified: 2026-06-30

This document defines native-side responsibilities. Native code owns renderer
backend resources and consumes engine-submitted immutable snapshots; Java engine
code remains the authority for gameplay state, collision decisions, and audio
events.

## Current vs Target

| Area | Current | Target |
| --- | --- | --- |
| Backend ownership | Native code owns renderer implementation and GPU resource lifetime. | Keep backend/resource ownership native while excluding authoritative gameplay state. |
| Scene data | Native code consumes Java-engine-submitted immutable scene snapshots. | Decode/validate submissions quickly and publish the latest pending snapshot for rendering. |
| Render thread | Render code treats submitted frame data as read-only. | Let rendering advance independently from Java update ticks without mutating world state. |
| Frame pacing / present | Java `Choreographer` is the active frame pacer. Native `renderFrame` currently presents through `vkQueuePresentKHR`; Swappy queue-present pacing is compiled behind `kUseSwappyFramePacing = false`. | Keep only one active frame pacer unless Swappy pacing is intentionally re-enabled and validated on device. |
| Instance model | JNI entry points delegate to one process-global native renderer instance. | Reuse the AAR across different apps, but do not assume multiple simultaneous renderer instances in one process without redesigning the native facade. |
| Audio backend | Native audio may implement playback for engine-approved events in the future. | Keep audio backend execution separate from gameplay/collision decision paths. |
| Locks/lifecycle | Synchronization, when required, protects native lifecycle/resources. | Scope locks to native resources and keep them independent of scene/world ownership. |

## Module Status

| Module | Boundary owner | Runtime responsibility | Status (2026-06-30) | Stabilization notes / roadmap |
| --- | --- | --- | --- | --- |
| `render/` | Native (`:VulkanRenderingEngine`) | Vulkan 2D backend, swapchain/pipeline/frame resources, draw submission from immutable snapshots. | **Active / In progress hardening** | Keep render path read-only against Java engine world state; Java remains the active frame pacer; continue tightening lifecycle error handling and resize/recreate sequencing. |
| `core/` | Shared native data layer | Scene/data structs consumed by JNI + render without gameplay mutation authority. | **Active / Stable contract** | Keep `core/` as pure data/utility boundary; avoid backend/device ownership leaking into this layer. |
| `jni/` | Native boundary adapter | Decode/validate Java engine payloads and publish latest pending snapshot for renderer consumption. | **Active / Stable with ongoing validation hardening** | Preserve fast-copy validation path; add explicit rejection metrics/logging for malformed payloads as follow-up. |
| `audio/` (planned native side) | Native backend (future) under engine event authority | Optional native playback backend for engine-approved audio events only. | **Not implemented yet (planned)** | Implement as isolated event-consumer module; no gameplay/collision direct triggers; track milestone as separate delivery from render readiness. |

## Native ownership

- Owns rendering backend implementation and GPU resource lifecycle.
- Consumes Java-engine-submitted immutable scene snapshots.
- May own audio backend playback implementation, but only for engine-approved audio events.

## Non-ownership (important)

- Does **not** own authoritative scene/world mutation.
- Does **not** call audio directly from gameplay/collision code paths.

## Runtime contract

- Scene submission should decode/validate quickly and publish latest pending snapshot.
- Render thread consumes latest snapshot independently and treats frame data as read-only.
- If locks are required, scope them to native lifecycle/resources, independent of gameplay ownership.
- `VK_ERROR_OUT_OF_DATE_KHR` marks resize/recreate pending. `VK_SUBOPTIMAL_KHR`
  is currently recorded in swapchain status but does not by itself force a
  recreate.
- The renderer links the Android Game SDK frame-pacing library, but
  `kUseSwappyFramePacing` is false. Current present timing is therefore shaped
  by Java `Choreographer` plus Vulkan `vkQueuePresentKHR` behavior.

## Directory intent

- `jni/`: JNI entry points + boundary decoding.
- `render/`: renderer implementation.
- `platform/android/`: Android platform adapters such as native window and Vulkan surface ownership.
- `core/`: shared native scene/data structures and utilities consumed by JNI and render layers.

## Reusable engine boundary

The reusable VulkanRenderEngine lives in `core/`, `render/`, `platform/android/`,
and the AAR-owned Java bridge package `com.hsw.vulkanrenderingengine.bridge`.
The `jni/` directory keeps JNI entry points thin and delegates to the native
facade contract.
Sample hosts, including `:smokehost`, must consume that AAR bridge instead of
adding host-package-specific JNI entry points to this native library.

The first-class native facade contract is `render/vulkan_render_engine.h`:
`init`, `resize`, `uploadTexture`, `submitSceneFrame`, `renderFrame`, and
`cleanup`. JNI function names must remain thin adapters over that contract.
The current facade has no per-instance handle, so it is intentionally a
single-renderer contract.

## Validation

- Ownership check: native sources must not introduce authoritative world-state,
  collision, gameplay decision, or direct audio-event ownership.
- JNI handoff check: JNI entry points should decode/validate Java payloads and
  publish immutable scene data for the renderer.
- Frame pacing check: `kUseSwappyFramePacing` describes whether native Swappy
  queue-present pacing is active; when false, docs and diagnostics should refer
  to Java `Choreographer` as the active pacer.
- Build check, when Gradle dependencies are cached or reachable:
  `./gradlew :VulkanRenderingEngine:assembleDebug`.
- Project bootstrap check: `./scripts/project-check.sh`.
