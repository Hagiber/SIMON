package com.hsw.simonapp.engine.loop.defaults;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.hsw.simonapp.engine.api.BlendMode;
import com.hsw.simonapp.engine.api.ScissorRect;
import com.hsw.simonapp.engine.api.TextureRegion;
import com.hsw.simonapp.engine.api.TouchInputEvent;
import com.hsw.simonapp.engine.input.TouchInputSnapshot;
import com.hsw.simonapp.engine.loop.core.CollisionPair;
import com.hsw.simonapp.engine.loop.core.DeterministicWorldUpdater;
import com.hsw.simonapp.engine.loop.core.FrameContext;
import com.hsw.simonapp.engine.loop.core.InputSnapshot;
import com.hsw.simonapp.engine.loop.core.NeutralInputSnapshot;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class SimpleWorldLogicTest {

    @Test
    public void update_movesEntityAndBouncesAtBounds() {
        SimpleWorldState worldState = new SimpleWorldState(Arrays.asList(
                new SimpleWorldState.EntityState(1, 0, SimpleWorldState.EntityState.ControlMode.HUMAN_TOUCH, 0, 0.9f, 0f, 1f, 0f, 1.0f, 0f, 0f, 0.1f)
        ));

        SimpleWorldState updatedWorldState = updateSimpleWorld(worldState,
                new FrameContext(1, 0.1f),
                NeutralInputSnapshot.INSTANCE);

        SimpleWorldState.EntityState entity = updatedWorldState.getEntities().get(0);
        assertEquals(0.85f, entity.getX(), 0.0001f);
        assertEquals(-1.0f, entity.getVelocityX(), 0.0001f);
        assertEquals(0.9f, worldState.getEntities().get(0).getX(), 0.0001f);
    }

    @Test
    public void update_usesViewportAdjustedVerticalBounds() {
        SimpleWorldUpdater simpleWorldUpdater = new SimpleWorldUpdater();
        simpleWorldUpdater.resizeViewport(100, 200);
        SimpleWorldState worldState = new SimpleWorldState(Arrays.asList(
                new SimpleWorldState.EntityState(1,
                        0,
                        SimpleWorldState.EntityState.ControlMode.AI,
                        0,
                        0f,
                        1.79f,
                        1f,
                        0f,
                        0f,
                        1.0f,
                        0f,
                        0.1f)
        ));

        SimpleWorldState updatedWorldState = updateSimpleWorld(simpleWorldUpdater,
                worldState,
                new FrameContext(1, 0.1f),
                NeutralInputSnapshot.INSTANCE);

        SimpleWorldState.EntityState entity = updatedWorldState.getEntities().get(0);
        assertEquals(1.8f, entity.getY(), 0.0001f);
        assertEquals(-1.0f, entity.getVelocityY(), 0.0001f);
    }

    @Test
    public void gameplay_clampsTouchTargetToReachableCenterLimit() {
        float collisionRadius = 0.1f;
        SimpleWorldState worldState = new SimpleWorldState(Arrays.asList(
                new SimpleWorldState.EntityState(1,
                        0,
                        SimpleWorldState.EntityState.ControlMode.HUMAN_TOUCH,
                        0,
                        0.85f,
                        0f,
                        1f,
                        0f,
                        1.0f,
                        0f,
                        0f,
                        collisionRadius)
        ));

        new GameplaySystem().apply(worldState, WorldBounds.BOUNDS, 0f);

        SimpleWorldState.EntityState entity = worldState.getEntities().get(0);
        assertEquals(0.0f, entity.getVelocityX(), 0.0001f);
        assertEquals(0.0f, entity.getVelocityY(), 0.0001f);
    }

    @Test
    public void update_advancesRotationAndAnimationState() {
        SimpleWorldState worldState = new SimpleWorldState(Arrays.asList(
                new SimpleWorldState.EntityState(1,
                        0,
                        SimpleWorldState.EntityState.ControlMode.AI,
                        0,
                        0f,
                        0f,
                        1f,
                        10.0f,
                        0.25f,
                        0f,
                        0f,
                        30.0f,
                        2.0f,
                        0.1f)
        ));

        SimpleWorldState updatedWorldState = updateSimpleWorld(worldState,
                new FrameContext(1, 0.5f),
                NeutralInputSnapshot.INSTANCE);

        SimpleWorldState.EntityState entity = updatedWorldState.getEntities().get(0);
        assertEquals(25.0f, entity.getRotationDeg(), 0.0001f);
        assertEquals(1.25f, entity.getAnimationState(), 0.0001f);
        assertEquals(0.25f, worldState.getEntities().get(0).getAnimationState(), 0.0001f);
    }

    @Test
    public void update_startsButtonPressAnimationForTouchedEntity() {
        WorldInteractionController interactionController = new WorldInteractionController();
        SimpleWorldUpdater simpleWorldUpdater = new SimpleWorldUpdater(interactionController);
        SimpleWorldState worldState = new SimpleWorldState(Arrays.asList(
                new SimpleWorldState.EntityState(3,
                        0,
                        SimpleWorldState.EntityState.ControlMode.AI,
                        0,
                        0f,
                        0f,
                        1f,
                        0f,
                        0f,
                        0f,
                        0f,
                        0f,
                        -4.0f,
                        0.2f,
                        BlendMode.ALPHA,
                        0,
                        0,
                        ScissorRect.disabled(),
                        TextureRegion.full(),
                        1.0f,
                        1.0f,
                        SimpleWorldState.EntityState.TouchInteraction.BUTTON_PRESS)
        ));
        TouchInputEvent touchDown = new TouchInputEvent(TouchInputEvent.Action.DOWN,
                9,
                50.0f,
                50.0f,
                100,
                100,
                1L);

        SimpleWorldState updatedWorldState = updateSimpleWorld(simpleWorldUpdater,
                worldState,
                new FrameContext(1, 0.0f),
                new TouchInputSnapshot(Collections.singletonList(touchDown)));

        SimpleWorldState.EntityState entity = updatedWorldState.getEntities().get(0);
        assertEquals(Integer.valueOf(3), interactionController.getSelectedEntityId());
        assertEquals(1.0f, entity.getAnimationState(), 0.0001f);
    }

    @Test
    public void update_countsDownButtonPressAnimationUntilIdle() {
        SimpleWorldState worldState = new SimpleWorldState(Arrays.asList(
                new SimpleWorldState.EntityState(3,
                        0,
                        SimpleWorldState.EntityState.ControlMode.AI,
                        0,
                        0f,
                        0f,
                        1f,
                        0f,
                        1.0f,
                        0f,
                        0f,
                        0f,
                        -4.0f,
                        0.2f,
                        BlendMode.ALPHA,
                        0,
                        0,
                        ScissorRect.disabled(),
                        TextureRegion.full(),
                        1.0f,
                        1.0f,
                        SimpleWorldState.EntityState.TouchInteraction.BUTTON_PRESS)
        ));

        SimpleWorldState partiallyUpdatedWorldState = updateSimpleWorld(worldState,
                new FrameContext(1, 0.15f),
                NeutralInputSnapshot.INSTANCE);
        SimpleWorldState finishedWorldState = updateSimpleWorld(partiallyUpdatedWorldState,
                new FrameContext(2, 0.2f),
                NeutralInputSnapshot.INSTANCE);

        assertEquals(0.4f, partiallyUpdatedWorldState.getEntities().get(0).getAnimationState(), 0.0001f);
        assertEquals(0.0f, finishedWorldState.getEntities().get(0).getAnimationState(), 0.0001f);
    }

    @Test
    public void update_appliesDrainedTouchInputToHumanEntity() {
        SimpleWorldState worldState = new SimpleWorldState(Arrays.asList(
                new SimpleWorldState.EntityState(1,
                        0,
                        SimpleWorldState.EntityState.ControlMode.HUMAN_TOUCH,
                        0,
                        0f,
                        0f,
                        1f,
                        0f,
                        0f,
                        0f,
                        0f,
                        0.1f)
        ));
        TouchInputEvent touchDown = new TouchInputEvent(TouchInputEvent.Action.DOWN,
                9,
                65.0f,
                50.0f,
                100,
                100,
                1L);

        SimpleWorldState updatedWorldState = updateSimpleWorld(worldState,
                new FrameContext(1, 0.1f),
                new TouchInputSnapshot(Collections.singletonList(touchDown)));

        SimpleWorldState.EntityState entity = updatedWorldState.getEntities().get(0);
        assertEquals(2.7f, entity.getVelocityX(), 0.0001f);
        assertEquals(0.0f, entity.getVelocityY(), 0.0001f);
        assertEquals(0.27f, entity.getX(), 0.0001f);
        assertEquals(0.0f, worldState.getEntities().get(0).getVelocityX(), 0.0001f);
    }

    @Test
    public void update_doesNotMoveHumanEntityWithNoTouchInteraction() {
        SimpleWorldState worldState = new SimpleWorldState(Arrays.asList(
                new SimpleWorldState.EntityState(1,
                        0,
                        SimpleWorldState.EntityState.ControlMode.HUMAN_TOUCH,
                        0,
                        0f,
                        0f,
                        1f,
                        0f,
                        0f,
                        0f,
                        0f,
                        0f,
                        0f,
                        0.1f,
                        BlendMode.ALPHA,
                        0,
                        0,
                        ScissorRect.disabled(),
                        TextureRegion.full(),
                        1.0f,
                        1.0f,
                        SimpleWorldState.EntityState.TouchInteraction.NONE)
        ));
        TouchInputEvent touchDown = new TouchInputEvent(TouchInputEvent.Action.DOWN,
                9,
                65.0f,
                50.0f,
                100,
                100,
                1L);

        SimpleWorldState updatedWorldState = updateSimpleWorld(worldState,
                new FrameContext(1, 0.1f),
                new TouchInputSnapshot(Collections.singletonList(touchDown)));

        SimpleWorldState.EntityState entity = updatedWorldState.getEntities().get(0);
        assertEquals(0.0f, entity.getVelocityX(), 0.0001f);
        assertEquals(0.0f, entity.getVelocityY(), 0.0001f);
        assertEquals(0.0f, entity.getX(), 0.0001f);
    }

    @Test
    public void update_selectsTouchedEntityById() {
        WorldInteractionController interactionController = new WorldInteractionController();
        int[] selectedEntityId = new int[]{-1};
        interactionController.setSelectedEntityListener(entityId -> selectedEntityId[0] = entityId);
        SimpleWorldUpdater simpleWorldUpdater = new SimpleWorldUpdater(interactionController);
        SimpleWorldState worldState = new SimpleWorldState(Arrays.asList(
                new SimpleWorldState.EntityState(1,
                        0,
                        SimpleWorldState.EntityState.ControlMode.AI,
                        0,
                        -0.4f,
                        -0.4f,
                        1f,
                        0f,
                        0f,
                        0f,
                        0f,
                        0.1f),
                new SimpleWorldState.EntityState(4,
                        0,
                        SimpleWorldState.EntityState.ControlMode.AI,
                        0,
                        0.48f,
                        0.48f,
                        1f,
                        0f,
                        0f,
                        0f,
                        0f,
                        0.2f)
        ));
        TouchInputEvent touchDown = new TouchInputEvent(TouchInputEvent.Action.DOWN,
                9,
                75.0f,
                25.0f,
                100,
                100,
                1L);

        updateSimpleWorld(simpleWorldUpdater,
                worldState,
                new FrameContext(1, 0.0f),
                new TouchInputSnapshot(Collections.singletonList(touchDown)));

        assertEquals(4, selectedEntityId[0]);
        assertEquals(Integer.valueOf(4), interactionController.getSelectedEntityId());
    }

    @Test
    public void update_ignoresEntityWithNoTouchInteractionWhenSelecting() {
        WorldInteractionController interactionController = new WorldInteractionController();
        int[] selectedEntityId = new int[]{-1};
        float[] touchedCoordinates = new float[]{Float.NaN, Float.NaN};
        interactionController.setSelectedEntityListener(entityId -> selectedEntityId[0] = entityId);
        interactionController.setWorldCoordinateTouchListener((x, y) -> {
            touchedCoordinates[0] = x;
            touchedCoordinates[1] = y;
        });
        SimpleWorldUpdater simpleWorldUpdater = new SimpleWorldUpdater(interactionController);
        SimpleWorldState worldState = new SimpleWorldState(Arrays.asList(
                new SimpleWorldState.EntityState(4,
                        0,
                        SimpleWorldState.EntityState.ControlMode.AI,
                        0,
                        0.28f,
                        0.0f,
                        1f,
                        0f,
                        0f,
                        0f,
                        0f,
                        0f,
                        0f,
                        0.2f,
                        BlendMode.ALPHA,
                        0,
                        0,
                        ScissorRect.disabled(),
                        TextureRegion.full(),
                        1.0f,
                        1.0f,
                        SimpleWorldState.EntityState.TouchInteraction.NONE)
        ));
        TouchInputEvent touchDown = new TouchInputEvent(TouchInputEvent.Action.DOWN,
                9,
                65.0f,
                50.0f,
                100,
                100,
                1L);

        updateSimpleWorld(simpleWorldUpdater,
                worldState,
                new FrameContext(1, 0.0f),
                new TouchInputSnapshot(Collections.singletonList(touchDown)));

        assertEquals(-1, selectedEntityId[0]);
        assertNull(interactionController.getSelectedEntityId());
        assertEquals(0.285f, touchedCoordinates[0], 0.0001f);
        assertEquals(0.0f, touchedCoordinates[1], 0.0001f);
    }

    @Test
    public void update_reversesSelectedEntityDirectionFromQueuedCommand() {
        WorldInteractionController interactionController = new WorldInteractionController();
        interactionController.selectEntity(2);
        assertTrue(interactionController.requestReverseSelectedEntityDirection());
        SimpleWorldUpdater simpleWorldUpdater = new SimpleWorldUpdater(interactionController);
        SimpleWorldState worldState = new SimpleWorldState(Arrays.asList(
                new SimpleWorldState.EntityState(2,
                        0,
                        SimpleWorldState.EntityState.ControlMode.AI,
                        0,
                        0f,
                        0f,
                        1f,
                        0f,
                        0.20f,
                        -0.35f,
                        0f,
                        0.1f)
        ));

        SimpleWorldState updatedWorldState = updateSimpleWorld(simpleWorldUpdater,
                worldState,
                new FrameContext(1, 0.0f),
                NeutralInputSnapshot.INSTANCE);

        SimpleWorldState.EntityState entity = updatedWorldState.getEntities().get(0);
        assertEquals(-0.20f, entity.getVelocityX(), 0.0001f);
        assertEquals(0.35f, entity.getVelocityY(), 0.0001f);
    }

    @Test
    public void update_reportsCoordinatesAndClearsSelectionWhenTouchMissesEntities() {
        WorldInteractionController interactionController = new WorldInteractionController();
        interactionController.selectEntity(2);
        float[] touchedCoordinates = new float[]{Float.NaN, Float.NaN};
        interactionController.setWorldCoordinateTouchListener((x, y) -> {
            touchedCoordinates[0] = x;
            touchedCoordinates[1] = y;
        });
        SimpleWorldUpdater simpleWorldUpdater = new SimpleWorldUpdater(interactionController);
        SimpleWorldState worldState = new SimpleWorldState(Arrays.asList(
                new SimpleWorldState.EntityState(2,
                        0,
                        SimpleWorldState.EntityState.ControlMode.AI,
                        0,
                        -0.8f,
                        -0.8f,
                        1f,
                        0f,
                        0f,
                        0f,
                        0f,
                        0.1f)
        ));
        TouchInputEvent touchDown = new TouchInputEvent(TouchInputEvent.Action.DOWN,
                9,
                75.0f,
                25.0f,
                100,
                100,
                1L);

        updateSimpleWorld(simpleWorldUpdater,
                worldState,
                new FrameContext(1, 0.0f),
                new TouchInputSnapshot(Collections.singletonList(touchDown)));

        assertNull(interactionController.getSelectedEntityId());
        assertEquals(0.475f, touchedCoordinates[0], 0.0001f);
        assertEquals(0.475f, touchedCoordinates[1], 0.0001f);
    }

    @Test
    public void collisionDetectionAndResolve_swapsVelocities() {
        SimpleWorldState worldState = new SimpleWorldState(Arrays.asList(
                new SimpleWorldState.EntityState(1, 0, SimpleWorldState.EntityState.ControlMode.HUMAN_TOUCH, 0, 0f, 0f, 1f, 0f, 0.2f, 0f, 0f, 0.2f),
                new SimpleWorldState.EntityState(2, 1, SimpleWorldState.EntityState.ControlMode.AI, 1, 0.1f, 0f, 1f, 0f, -0.3f, 0f, 0f, 0.2f)
        ));

        List<CollisionPair> collisions = new SimpleCollisionDetector().detect(new FrameContext(1, 0.016f), worldState);
        assertEquals(1, collisions.size());
        assertTrue(collisions.stream().anyMatch(pair -> pair.getEntityA() == 1 && pair.getEntityB() == 2));

        new SimpleCollisionResolver().resolve(new FrameContext(1, 0.016f), worldState, collisions);

        assertEquals(-0.3f, worldState.getEntities().get(0).getVelocityX(), 0.0001f);
        assertEquals(0.2f, worldState.getEntities().get(1).getVelocityX(), 0.0001f);
    }

    private static SimpleWorldState updateSimpleWorld(SimpleWorldState worldState,
                                                      FrameContext frameContext,
                                                      InputSnapshot inputSnapshot) {
        SimpleWorldUpdater simpleWorldUpdater = new SimpleWorldUpdater();
        return updateSimpleWorld(simpleWorldUpdater, worldState, frameContext, inputSnapshot);
    }

    private static SimpleWorldState updateSimpleWorld(SimpleWorldUpdater simpleWorldUpdater,
                                                      SimpleWorldState worldState,
                                                      FrameContext frameContext,
                                                      InputSnapshot inputSnapshot) {
        DeterministicWorldUpdater updater = new DeterministicWorldUpdater(simpleWorldUpdater,
                simpleWorldUpdater,
                simpleWorldUpdater);
        return (SimpleWorldState) updater.update(frameContext, worldState, inputSnapshot);
    }
}


