package com.hsw.etalonapp.engine.loop.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import com.hsw.etalonapp.engine.audio.AudioEvent;
import com.hsw.etalonapp.engine.audio.AudioEventQueue;
import com.hsw.etalonapp.engine.audio.AudioEventType;
import com.hsw.etalonapp.engine.audio.AudioSubsystem;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GameLoopTest {

    @Test
    public void deterministicWorldUpdater_runsInputGameplayAndPhysicsInOrder() {
        List<String> steps = new ArrayList<>();
        TestWorldState initialWorldState = new TestWorldState("initial");
        TestWorldState inputAppliedWorldState = new TestWorldState("input-applied");
        TestWorldState gameplayWorldState = new TestWorldState("gameplay");
        TestWorldState physicsWorldState = new TestWorldState("physics");

        WorldUpdater updater = new DeterministicWorldUpdater(
                (frameContext, currentWorldState, inputSnapshot) -> {
                    steps.add("1-input-apply");
                    assertSame(initialWorldState, currentWorldState);
                    return inputAppliedWorldState;
                },
                (frameContext, currentWorldState) -> {
                    steps.add("2-gameplay");
                    assertSame(inputAppliedWorldState, currentWorldState);
                    return gameplayWorldState;
                },
                (frameContext, currentWorldState) -> {
                    steps.add("3-physics");
                    assertSame(gameplayWorldState, currentWorldState);
                    return physicsWorldState;
                });

        WorldState updatedWorldState = updater.update(new FrameContext(1, 0.016f),
                initialWorldState,
                new TestInputSnapshot());

        assertSame(physicsWorldState, updatedWorldState);
        assertEquals("1-input-apply", steps.get(0));
        assertEquals("2-gameplay", steps.get(1));
        assertEquals("3-physics", steps.get(2));
        assertEquals(3, steps.size());
    }

    @Test
    public void runFrame_callsFixedUpdateStagesInExpectedOrderBeforeRender() {
        List<String> steps = new ArrayList<>();
        TestWorldState initialWorldState = new TestWorldState("initial");
        TestWorldState updatedWorldState = new TestWorldState("updated");

        GameLoop gameLoop = new GameLoop(
                initialWorldState,
                frameContext -> {
                    steps.add("1-input");
                    return new TestInputSnapshot();
                },
                (frameContext, currentWorldState, inputSnapshot) -> {
                    steps.add("2-update");
                    assertEquals(GameLoop.FIXED_UPDATE_DELTA_SECONDS,
                            frameContext.getDeltaSeconds(),
                            0.000001f);
                    return updatedWorldState;
                },
                (frameContext, worldState) -> {
                    steps.add("3-collision-detect");
                    assertSame(updatedWorldState, worldState);
                    return Collections.singletonList(new CollisionPair(1, 2));
                },
                (frameContext, worldState, collisions) -> steps.add("4-collision-resolve"),
                (frameContext, worldState, collisions) -> steps.add("5-event-emit"),
                AudioSubsystem.noOp(),
                (frameContext, renderFrameState) -> {
                    steps.add("6-render");
                    assertSame(initialWorldState, renderFrameState.getPreviousWorldState());
                    assertSame(updatedWorldState, renderFrameState.getCurrentWorldState());
                    assertEquals(0.0f, renderFrameState.getInterpolationAlpha(), 0.000001f);
                },
                GameLoop.FIXED_UPDATE_DELTA_SECONDS
        );

        gameLoop.runFrame(new FrameContext(1, GameLoop.FIXED_UPDATE_DELTA_SECONDS));

        assertEquals("1-input", steps.get(0));
        assertEquals("2-update", steps.get(1));
        assertEquals("3-collision-detect", steps.get(2));
        assertEquals("4-collision-resolve", steps.get(3));
        assertEquals("5-event-emit", steps.get(4));
        assertEquals("6-render", steps.get(5));
        assertEquals(6, steps.size());
    }

    @Test
    public void runFrame_accumulatesDeltaBeforeRunningFixedUpdate() {
        List<String> steps = new ArrayList<>();
        TestWorldState initialWorldState = new TestWorldState("initial");
        TestWorldState updatedWorldState = new TestWorldState("updated");

        GameLoop gameLoop = new GameLoop(
                initialWorldState,
                frameContext -> {
                    steps.add("input");
                    return new TestInputSnapshot();
                },
                (frameContext, currentWorldState, inputSnapshot) -> {
                    steps.add("update");
                    return updatedWorldState;
                },
                (frameContext, worldState) -> {
                    steps.add("collision-detect");
                    return Collections.singletonList(new CollisionPair(1, 2));
                },
                (frameContext, worldState, collisions) -> steps.add("collision-resolve"),
                (frameContext, renderFrameState) -> steps.add("render:"
                        + ((TestWorldState) renderFrameState.getPreviousWorldState()).name
                        + "->"
                        + ((TestWorldState) renderFrameState.getCurrentWorldState()).name
                        + "@"
                        + renderFrameState.getInterpolationAlpha())
        );

        float halfTick = GameLoop.FIXED_UPDATE_DELTA_SECONDS * 0.5f;
        gameLoop.runFrame(new FrameContext(1, halfTick));
        gameLoop.runFrame(new FrameContext(2, halfTick));

        assertEquals("render:initial->initial@0.5", steps.get(0));
        assertEquals("input", steps.get(1));
        assertEquals("update", steps.get(2));
        assertEquals("collision-detect", steps.get(3));
        assertEquals("collision-resolve", steps.get(4));
        assertEquals("render:initial->updated@0.0", steps.get(5));
        assertEquals(6, steps.size());
    }

    @Test
    public void runFrame_runsMultipleFixedUpdatesBeforeSingleRender() {
        List<Float> updateDeltas = new ArrayList<>();
        List<String> renderStates = new ArrayList<>();
        TestWorldState initialWorldState = new TestWorldState("initial");
        int[] updateCount = {0};

        GameLoop gameLoop = new GameLoop(
                initialWorldState,
                frameContext -> new TestInputSnapshot(),
                (frameContext, currentWorldState, inputSnapshot) -> {
                    updateDeltas.add(frameContext.getDeltaSeconds());
                    updateCount[0]++;
                    return new TestWorldState("updated-" + updateCount[0]);
                },
                (frameContext, worldState) -> Collections.emptyList(),
                (frameContext, worldState, collisions) -> {
                },
                (frameContext, renderFrameState) -> {
                    renderStates.add(((TestWorldState) renderFrameState.getPreviousWorldState()).name
                            + "->"
                            + ((TestWorldState) renderFrameState.getCurrentWorldState()).name);
                    assertEquals(0.4f, renderFrameState.getInterpolationAlpha(), 0.000001f);
                }
        );

        gameLoop.runFrame(new FrameContext(1, GameLoop.FIXED_UPDATE_DELTA_SECONDS * 2.4f));

        assertEquals(2, updateDeltas.size());
        assertEquals(GameLoop.FIXED_UPDATE_DELTA_SECONDS, updateDeltas.get(0), 0.000001f);
        assertEquals(GameLoop.FIXED_UPDATE_DELTA_SECONDS, updateDeltas.get(1), 0.000001f);
        assertEquals(1, renderStates.size());
        assertEquals("updated-1->updated-2", renderStates.get(0));
    }

    @Test
    public void runFrame_clampsLargeFrameDeltaBeforeAccumulatingAndRendering() {
        List<Float> updateDeltas = new ArrayList<>();
        List<Float> renderDeltas = new ArrayList<>();
        List<Float> renderAlphas = new ArrayList<>();

        GameLoop gameLoop = new GameLoop(
                new TestWorldState("initial"),
                frameContext -> new TestInputSnapshot(),
                (frameContext, currentWorldState, inputSnapshot) -> {
                    updateDeltas.add(frameContext.getDeltaSeconds());
                    return new TestWorldState("updated");
                },
                (frameContext, worldState) -> Collections.emptyList(),
                (frameContext, worldState, collisions) -> {
                },
                (frameContext, renderFrameState) -> {
                    renderDeltas.add(frameContext.getDeltaSeconds());
                    renderAlphas.add(renderFrameState.getInterpolationAlpha());
                },
                0.2d
        );

        gameLoop.runFrame(new FrameContext(1, 3.0f));

        assertEquals(1, updateDeltas.size());
        assertEquals(0.2f, updateDeltas.get(0), 0.000001f);
        assertEquals(1, renderDeltas.size());
        assertEquals(GameLoop.MAX_FRAME_DELTA_SECONDS, renderDeltas.get(0), 0.000001f);
        assertEquals(0.25f, renderAlphas.get(0), 0.000001f);
    }

    @Test
    public void runFrame_limitsFixedUpdatesPerFrameAndDropsExcessTickBacklog() {
        int[] updateCount = {0};
        List<Float> renderAlphas = new ArrayList<>();

        GameLoop gameLoop = new GameLoop(
                new TestWorldState("initial"),
                frameContext -> new TestInputSnapshot(),
                (frameContext, currentWorldState, inputSnapshot) -> {
                    updateCount[0]++;
                    return new TestWorldState("updated-" + updateCount[0]);
                },
                (frameContext, worldState) -> Collections.emptyList(),
                (frameContext, worldState, collisions) -> {
                },
                (frameContext, renderFrameState) -> renderAlphas.add(renderFrameState.getInterpolationAlpha())
        );

        gameLoop.runFrame(new FrameContext(1, GameLoop.MAX_FRAME_DELTA_SECONDS));
        gameLoop.runFrame(new FrameContext(2, 0.0f));

        assertEquals(GameLoop.MAX_UPDATE_TICKS_PER_FRAME, updateCount[0]);
        assertEquals(2, renderAlphas.size());
        assertEquals(0.0f, renderAlphas.get(0), 0.000001f);
        assertEquals(0.0f, renderAlphas.get(1), 0.000001f);
    }

    @Test
    public void runFrame_publishesCollisionAudioEventsBeforeRender() {
        List<String> steps = new ArrayList<>();
        AudioEventQueue audioEventQueue = new AudioEventQueue();
        AudioSubsystem audioSubsystem = () -> {
            steps.add("5-audio");
            List<AudioEvent> audioEvents = audioEventQueue.drain();
            assertEquals(1, audioEvents.size());
            assertEquals(AudioEventType.COLLISION_SOUND, audioEvents.get(0).getType());
        };

        GameLoop gameLoop = new GameLoop(
                new TestWorldState("initial"),
                frameContext -> {
                    steps.add("1-input");
                    return new TestInputSnapshot();
                },
                (frameContext, currentWorldState, inputSnapshot) -> {
                    steps.add("2-update");
                    return new TestWorldState("updated");
                },
                (frameContext, worldState) -> {
                    steps.add("3-collision-detect");
                    return Collections.singletonList(new CollisionPair(2, 1));
                },
                (frameContext, worldState, collisions) -> steps.add("4-collision-resolve"),
                audioEventQueue,
                audioSubsystem,
                (frameContext, renderFrameState) -> steps.add("6-render")
        );

        gameLoop.runFrame(new FrameContext(1, GameLoop.FIXED_UPDATE_DELTA_SECONDS));

        assertEquals("1-input", steps.get(0));
        assertEquals("2-update", steps.get(1));
        assertEquals("3-collision-detect", steps.get(2));
        assertEquals("4-collision-resolve", steps.get(3));
        assertEquals("5-audio", steps.get(4));
        assertEquals("6-render", steps.get(5));
        assertEquals(6, steps.size());
    }

    @Test
    public void runFrame_reportsTimingForMeasuredStages() {
        long[] recordedFrameIndex = {-1L};
        int[] recordedUpdateTicks = {-1};
        long[] recordedFrameTotalNanos = {-1L};
        AudioEventQueue audioEventQueue = new AudioEventQueue();

        GameLoop gameLoop = new GameLoop(
                new TestWorldState("initial"),
                frameContext -> new TestInputSnapshot(),
                (frameContext, currentWorldState, inputSnapshot) -> new TestWorldState("updated"),
                (frameContext, worldState) -> Collections.emptyList(),
                (frameContext, worldState, collisions) -> {
                },
                audioEventQueue,
                AudioSubsystem.noOp(),
                (frameContext, renderFrameState) -> {
                },
                (frameIndex,
                 deltaSeconds,
                 updateTicks,
                 updateNanos,
                 audioNanos,
                 snapshotBuildNanos,
                 nativeRenderNanos,
                 renderTotalNanos,
                 frameTotalNanos) -> {
                    recordedFrameIndex[0] = frameIndex;
                    recordedUpdateTicks[0] = updateTicks;
                    recordedFrameTotalNanos[0] = frameTotalNanos;
                    assertTrue(updateNanos >= 0L);
                    assertTrue(audioNanos >= 0L);
                    assertTrue(snapshotBuildNanos >= 0L);
                    assertTrue(nativeRenderNanos >= 0L);
                    assertTrue(renderTotalNanos >= 0L);
                }
        );

        gameLoop.runFrame(new FrameContext(7, GameLoop.FIXED_UPDATE_DELTA_SECONDS));

        assertEquals(7L, recordedFrameIndex[0]);
        assertEquals(1, recordedUpdateTicks[0]);
        assertTrue(recordedFrameTotalNanos[0] >= 0L);
    }

    @Test
    public void runFrame_rendersStateAfterCollisionResolveAndEventEmission() {
        List<String> steps = new ArrayList<>();
        TestWorldState initialWorldState = new TestWorldState("initial");
        TestWorldState updatedWorldState = new TestWorldState("updated");

        GameLoop gameLoop = new GameLoop(
                initialWorldState,
                frameContext -> new TestInputSnapshot(),
                (frameContext, currentWorldState, inputSnapshot) -> updatedWorldState,
                (frameContext, worldState) -> Collections.singletonList(new CollisionPair(1, 2)),
                (frameContext, worldState, collisions) -> {
                    steps.add("1-collision-resolve");
                    ((TestWorldState) worldState).collisionResolved = true;
                },
                (frameContext, worldState, collisions) -> {
                    steps.add("2-event-emit");
                    assertTrue(((TestWorldState) worldState).collisionResolved);
                },
                AudioSubsystem.noOp(),
                (frameContext, renderFrameState) -> {
                    steps.add("3-render");
                    assertTrue(((TestWorldState) renderFrameState.getCurrentWorldState()).collisionResolved);
                },
                GameLoop.FIXED_UPDATE_DELTA_SECONDS
        );

        gameLoop.runFrame(new FrameContext(1, GameLoop.FIXED_UPDATE_DELTA_SECONDS));

        assertEquals("1-collision-resolve", steps.get(0));
        assertEquals("2-event-emit", steps.get(1));
        assertEquals("3-render", steps.get(2));
        assertEquals(3, steps.size());
    }

    private static final class TestInputSnapshot implements InputSnapshot {
    }

    private static final class TestWorldState implements WorldState {
        private final String name;
        private boolean collisionResolved;

        private TestWorldState(String name) {
            this.name = name;
        }
    }
}


