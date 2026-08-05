package com.hsw.simonapp.engine.loop.core;

import com.hsw.simonapp.engine.audio.AudioEventSink;
import com.hsw.simonapp.engine.audio.AudioSubsystem;

import java.util.List;
import java.util.Objects;

/**
 * Host-side game loop skeleton.
 *
 * Sequence per update tick:
 * 1) input queue drain into a tick-local snapshot
 * 2) input application
 * 3) AI/gameplay decision
 * 4) physics integration
 * 5) collision detection
 * 6) collision resolve
 * 7) update-side event publication (audio/VFX/log intent)
 *
 * Audio consumes queued update events separately before render.
 * Render runs once per host frame after all allowed accumulated fixed update ticks.
 * Render receives a prev/curr state pair plus an interpolation alpha and must treat both states as read-only.
 * Large host frame deltas are clamped and per-frame update ticks are capped to prevent catch-up spirals.
 */
public final class GameLoop {

    private static final double DEFAULT_FIXED_UPDATE_DELTA_SECONDS = 1.0d / 60.0d;

    public static final float FIXED_UPDATE_DELTA_SECONDS = (float) DEFAULT_FIXED_UPDATE_DELTA_SECONDS;
    public static final float MAX_FRAME_DELTA_SECONDS = 0.250f;
    public static final int MAX_UPDATE_TICKS_PER_FRAME = 8;

    private static final double INTERPOLATION_ALPHA_EPSILON = 0.000001d;

    private final InputReader inputReader;
    private final WorldUpdater worldUpdater;
    private final CollisionDetector collisionDetector;
    private final CollisionResolver collisionResolver;
    private final UpdateEventEmitter updateEventEmitter;
    private final AudioSubsystem audioSubsystem;
    private final FrameRenderer frameRenderer;
    private final FrameTimingSink frameTimingSink;
    private final double fixedUpdateDeltaSeconds;

    private final WorldStateBuffer worldStateBuffer;
    private long updateTickIndex = 0L;
    private double accumulatedSeconds = 0.0d;

    public GameLoop(WorldState initialWorldState,
                    InputReader inputReader,
                    WorldUpdater worldUpdater,
                    CollisionDetector collisionDetector,
                    CollisionResolver collisionResolver,
                    FrameRenderer frameRenderer) {
        this(initialWorldState,
                inputReader,
                worldUpdater,
                collisionDetector,
                collisionResolver,
                UpdateEventEmitter.ignoring(),
                AudioSubsystem.noOp(),
                frameRenderer,
                DEFAULT_FIXED_UPDATE_DELTA_SECONDS);
    }

    public GameLoop(WorldState initialWorldState,
                    InputReader inputReader,
                    WorldUpdater worldUpdater,
                    CollisionDetector collisionDetector,
                    CollisionResolver collisionResolver,
                    AudioEventSink audioEventSink,
                    AudioSubsystem audioSubsystem,
                    FrameRenderer frameRenderer) {
        this(initialWorldState,
                inputReader,
                worldUpdater,
                collisionDetector,
                collisionResolver,
                UpdateEventEmitter.ignoring(),
                audioSubsystem,
                frameRenderer,
                FrameTimingSink.NO_OP,
                DEFAULT_FIXED_UPDATE_DELTA_SECONDS);
    }

    public GameLoop(WorldState initialWorldState,
                    InputReader inputReader,
                    WorldUpdater worldUpdater,
                    CollisionDetector collisionDetector,
                    CollisionResolver collisionResolver,
                    AudioEventSink audioEventSink,
                    AudioSubsystem audioSubsystem,
                    FrameRenderer frameRenderer,
                    FrameTimingSink frameTimingSink) {
        this(initialWorldState,
                inputReader,
                worldUpdater,
                collisionDetector,
                collisionResolver,
                UpdateEventEmitter.ignoring(),
                audioSubsystem,
                frameRenderer,
                frameTimingSink,
                DEFAULT_FIXED_UPDATE_DELTA_SECONDS);
    }

    GameLoop(WorldState initialWorldState,
             InputReader inputReader,
             WorldUpdater worldUpdater,
             CollisionDetector collisionDetector,
             CollisionResolver collisionResolver,
             FrameRenderer frameRenderer,
             double fixedUpdateDeltaSeconds) {
        this(initialWorldState,
                inputReader,
                worldUpdater,
                collisionDetector,
                collisionResolver,
                UpdateEventEmitter.ignoring(),
                AudioSubsystem.noOp(),
                frameRenderer,
                FrameTimingSink.NO_OP,
                fixedUpdateDeltaSeconds);
    }

    GameLoop(WorldState initialWorldState,
             InputReader inputReader,
             WorldUpdater worldUpdater,
             CollisionDetector collisionDetector,
             CollisionResolver collisionResolver,
             UpdateEventEmitter updateEventEmitter,
             AudioSubsystem audioSubsystem,
             FrameRenderer frameRenderer,
             double fixedUpdateDeltaSeconds) {
        this(initialWorldState,
                inputReader,
                worldUpdater,
                collisionDetector,
                collisionResolver,
                updateEventEmitter,
                audioSubsystem,
                frameRenderer,
                FrameTimingSink.NO_OP,
                fixedUpdateDeltaSeconds);
    }

    GameLoop(WorldState initialWorldState,
             InputReader inputReader,
             WorldUpdater worldUpdater,
             CollisionDetector collisionDetector,
             CollisionResolver collisionResolver,
             UpdateEventEmitter updateEventEmitter,
             AudioSubsystem audioSubsystem,
             FrameRenderer frameRenderer,
             FrameTimingSink frameTimingSink,
             double fixedUpdateDeltaSeconds) {
        this.worldStateBuffer = new WorldStateBuffer(initialWorldState);
        this.inputReader = Objects.requireNonNull(inputReader, "inputReader");
        this.worldUpdater = Objects.requireNonNull(worldUpdater, "worldUpdater");
        this.collisionDetector = Objects.requireNonNull(collisionDetector, "collisionDetector");
        this.collisionResolver = Objects.requireNonNull(collisionResolver, "collisionResolver");
        this.updateEventEmitter = Objects.requireNonNull(updateEventEmitter, "updateEventEmitter");
        this.audioSubsystem = Objects.requireNonNull(audioSubsystem, "audioSubsystem");
        this.frameRenderer = Objects.requireNonNull(frameRenderer, "frameRenderer");
        this.frameTimingSink = Objects.requireNonNull(frameTimingSink, "frameTimingSink");
        if (fixedUpdateDeltaSeconds <= 0.0d) {
            throw new IllegalArgumentException("fixedUpdateDeltaSeconds must be positive");
        }
        this.fixedUpdateDeltaSeconds = fixedUpdateDeltaSeconds;
    }

    GameLoop(WorldState initialWorldState,
             InputReader inputReader,
             WorldUpdater worldUpdater,
             CollisionDetector collisionDetector,
             CollisionResolver collisionResolver,
             AudioEventSink audioEventSink,
             AudioSubsystem audioSubsystem,
             FrameRenderer frameRenderer,
             double fixedUpdateDeltaSeconds) {
        this(initialWorldState,
                inputReader,
                worldUpdater,
                collisionDetector,
                collisionResolver,
                UpdateEventEmitter.ignoring(),
                audioSubsystem,
                frameRenderer,
                FrameTimingSink.NO_OP,
                fixedUpdateDeltaSeconds);
    }

    public void runFrame(FrameContext renderContext) {
        long frameStartNanos = System.nanoTime();
        double frameDeltaSeconds = clampFrameDelta(renderContext.getDeltaSeconds());
        FrameContext clampedRenderContext = new FrameContext(renderContext.getFrameIndex(), (float) frameDeltaSeconds);
        accumulatedSeconds += frameDeltaSeconds;

        int updateTicksThisFrame = 0;
        long updateStartNanos = System.nanoTime();
        while (accumulatedSeconds >= fixedUpdateDeltaSeconds
                && updateTicksThisFrame < MAX_UPDATE_TICKS_PER_FRAME) {
            runUpdateTick(new FrameContext(updateTickIndex++, (float) fixedUpdateDeltaSeconds));
            accumulatedSeconds -= fixedUpdateDeltaSeconds;
            updateTicksThisFrame++;
        }
        long updateNanos = System.nanoTime() - updateStartNanos;
        discardExcessUpdateBacklog(updateTicksThisFrame);

        long audioStartNanos = System.nanoTime();
        audioSubsystem.consumePendingEvents();
        long audioNanos = System.nanoTime() - audioStartNanos;

        FrameTimingAccumulator renderTiming = new FrameTimingAccumulator();
        long renderStartNanos = System.nanoTime();
        frameRenderer.render(clampedRenderContext,
                worldStateBuffer.toRenderFrameState(calculateInterpolationAlpha()),
                renderTiming);
        long renderTotalNanos = System.nanoTime() - renderStartNanos;

        frameTimingSink.recordFrameTiming(clampedRenderContext.getFrameIndex(),
                clampedRenderContext.getDeltaSeconds(),
                updateTicksThisFrame,
                updateNanos,
                audioNanos,
                renderTiming.getSnapshotBuildNanos(),
                renderTiming.getNativeRenderNanos(),
                renderTotalNanos,
                System.nanoTime() - frameStartNanos);
    }

    public WorldState getCurrentWorldState() {
        return worldStateBuffer.getCurrentWorldState();
    }

    public void replaceWorldState(WorldState worldState) {
        worldStateBuffer.resetTo(worldState);
        accumulatedSeconds = 0.0d;
    }

    private void runUpdateTick(FrameContext updateContext) {
        InputSnapshot inputSnapshot = inputReader.read(updateContext);
        WorldState currentWorldState = worldStateBuffer.getCurrentWorldState();
        WorldState nextWorldState = worldUpdater.update(updateContext, currentWorldState, inputSnapshot);
        List<CollisionPair> collisions = collisionDetector.detect(updateContext, nextWorldState);
        collisionResolver.resolve(updateContext, nextWorldState, collisions);
        updateEventEmitter.emit(updateContext, nextWorldState, collisions);
        worldStateBuffer.advanceTo(nextWorldState);
    }

    private float calculateInterpolationAlpha() {
        double alpha = accumulatedSeconds / fixedUpdateDeltaSeconds;
        if (alpha <= INTERPOLATION_ALPHA_EPSILON) {
            return 0.0f;
        }
        if (alpha >= 1.0d - INTERPOLATION_ALPHA_EPSILON) {
            return 1.0f;
        }
        return (float) Math.max(0.0d, Math.min(1.0d, alpha));
    }

    private void discardExcessUpdateBacklog(int updateTicksThisFrame) {
        if (updateTicksThisFrame < MAX_UPDATE_TICKS_PER_FRAME
                || accumulatedSeconds < fixedUpdateDeltaSeconds) {
            return;
        }
        accumulatedSeconds = accumulatedSeconds % fixedUpdateDeltaSeconds;
    }

    private static double clampFrameDelta(float deltaSeconds) {
        if (deltaSeconds <= 0.0f) {
            return 0.0d;
        }
        return Math.min(deltaSeconds, MAX_FRAME_DELTA_SECONDS);
    }
}


