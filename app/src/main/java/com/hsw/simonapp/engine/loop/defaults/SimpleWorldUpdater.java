package com.hsw.simonapp.engine.loop.defaults;

import com.hsw.simonapp.engine.api.OrthoCamera;
import com.hsw.simonapp.engine.loop.core.FrameContext;
import com.hsw.simonapp.engine.loop.core.GameplayDecider;
import com.hsw.simonapp.engine.loop.core.InputApplier;
import com.hsw.simonapp.engine.loop.core.InputSnapshot;
import com.hsw.simonapp.engine.loop.core.PhysicsIntegrator;
import com.hsw.simonapp.engine.loop.core.WorldState;
import com.hsw.simonapp.engine.api.TouchInputEvent;
import com.hsw.simonapp.engine.input.TouchInputSnapshot;

import java.util.Objects;

public final class SimpleWorldUpdater implements InputApplier, GameplayDecider, PhysicsIntegrator {

    private static final float SPRITE_HALF_EXTENT = 0.35f;
    private static final float TOUCH_HIT_MARGIN = 0.04f;
    private static final float DEGREES_TO_RADIANS = 0.0174532925199432957f;
    private static final float BUTTON_PRESS_ANIMATION_START_STATE = 1.0f;

    private final TouchInputStateReducer touchInputStateReducer;
    private final GameplaySystem gameplaySystem;
    private final PhysicsSystem physicsSystem;
    private final WorldBounds worldBounds;
    private final WorldInteractionController worldInteractionController;

    public SimpleWorldUpdater() {
        this(new WorldInteractionController());
    }

    public SimpleWorldUpdater(WorldInteractionController worldInteractionController) {
        this(WorldBounds.defaults(), worldInteractionController);
    }

    private SimpleWorldUpdater(WorldBounds worldBounds,
                               WorldInteractionController worldInteractionController) {
        this(new TouchInputStateReducer(worldBounds),
                new GameplaySystem(worldBounds),
                new PhysicsSystem(worldBounds),
                worldBounds,
                worldInteractionController);
    }

    SimpleWorldUpdater(TouchInputStateReducer touchInputStateReducer,
                       GameplaySystem gameplaySystem,
                       PhysicsSystem physicsSystem) {
        this(touchInputStateReducer,
                gameplaySystem,
                physicsSystem,
                WorldBounds.defaults(),
                new WorldInteractionController());
    }

    private SimpleWorldUpdater(TouchInputStateReducer touchInputStateReducer,
                               GameplaySystem gameplaySystem,
                               PhysicsSystem physicsSystem,
                               WorldBounds worldBounds,
                               WorldInteractionController worldInteractionController) {
        this.touchInputStateReducer = touchInputStateReducer;
        this.gameplaySystem = gameplaySystem;
        this.physicsSystem = physicsSystem;
        this.worldBounds = worldBounds;
        this.worldInteractionController = worldInteractionController;
    }

    public void resizeViewport(int width, int height) {
        worldBounds.setViewportSize(width, height);
    }

    public void resizeViewport(int width, int height, OrthoCamera camera) {
        worldBounds.setViewportSize(width,
                height,
                Objects.requireNonNull(camera, "camera").getZoom());
    }

    @Override
    public WorldState applyInput(FrameContext frameContext, WorldState currentWorldState, InputSnapshot inputSnapshot) {
        SimpleWorldState nextWorldState = copySimpleWorldState(currentWorldState);
        if (nextWorldState == null) {
            return currentWorldState;
        }
        applyTouchSelections(inputSnapshot, nextWorldState);
        touchInputStateReducer.reduce(inputSnapshot);
        return nextWorldState;
    }

    @Override
    public WorldState decideGameplay(FrameContext frameContext, WorldState currentWorldState) {
        SimpleWorldState nextWorldState = copySimpleWorldState(currentWorldState);
        if (nextWorldState == null) {
            return currentWorldState;
        }
        gameplaySystem.apply(nextWorldState,
                touchInputStateReducer.getActiveTouchTargetX(),
                touchInputStateReducer.getActiveTouchTargetY());
        applyQueuedWorldCommands(nextWorldState);
        return nextWorldState;
    }

    @Override
    public WorldState integratePhysics(FrameContext frameContext, WorldState currentWorldState) {
        SimpleWorldState nextWorldState = copySimpleWorldState(currentWorldState);
        if (nextWorldState == null) {
            return currentWorldState;
        }

        physicsSystem.integrate(nextWorldState, frameContext.getDeltaSeconds());
        return nextWorldState;
    }

    private static SimpleWorldState copySimpleWorldState(WorldState worldState) {
        if (!(worldState instanceof SimpleWorldState)) {
            return null;
        }
        return ((SimpleWorldState) worldState).copy();
    }

    private void applyTouchSelections(InputSnapshot inputSnapshot, SimpleWorldState worldState) {
        if (!(inputSnapshot instanceof TouchInputSnapshot)) {
            return;
        }

        TouchInputSnapshot touchInputSnapshot = (TouchInputSnapshot) inputSnapshot;
        for (TouchInputEvent event : touchInputSnapshot.getEvents()) {
            if (event.getAction() == TouchInputEvent.Action.DOWN) {
                selectTouchedEntity(event, worldState);
            }
        }
    }

    private void selectTouchedEntity(TouchInputEvent event, SimpleWorldState worldState) {
        float touchX = touchInputStateReducer.toWorldX(event);
        float touchY = touchInputStateReducer.toWorldY(event);
        SimpleWorldState.EntityState selectedEntity = null;
        for (SimpleWorldState.EntityState entity : worldState.getEntities()) {
            if (isTouchInteractive(entity)
                    && containsTouch(entity, touchX, touchY)
                    && rendersAbove(entity, selectedEntity)) {
                selectedEntity = entity;
            }
        }

        if (selectedEntity != null) {
            worldInteractionController.selectEntity(selectedEntity.getEntityId());
            switch (selectedEntity.getTouchInteraction()) {
                case MOVE_TO_TOUCH:
                    touchInputStateReducer.startTouchTarget(event);
                    break;
                case BUTTON_PRESS:
                    touchInputStateReducer.clearTouchTarget();
                    worldInteractionController.requestButtonPress(selectedEntity.getEntityId());
                    break;
                case SELECT:
                case NONE:
                default:
                    touchInputStateReducer.clearTouchTarget();
                    break;
            }
        } else {
            touchInputStateReducer.clearTouchTarget();
            worldInteractionController.showWorldCoordinates(touchX, touchY);
        }
    }

    private static boolean isTouchInteractive(SimpleWorldState.EntityState entity) {
        return entity.getTouchInteraction() != SimpleWorldState.EntityState.TouchInteraction.NONE;
    }

    private static boolean containsTouch(SimpleWorldState.EntityState entity, float touchX, float touchY) {
        float dx = touchX - entity.getX();
        float dy = touchY - entity.getY();
        float angle = entity.getRotationDeg() * DEGREES_TO_RADIANS;
        float c = (float) Math.cos(angle);
        float s = (float) Math.sin(angle);
        float localX = c * dx + s * dy;
        float localY = -s * dx + c * dy;
        float halfWidth = Math.max(entity.getCollisionRadius(), Math.abs(entity.getScaleX()) * SPRITE_HALF_EXTENT)
                + TOUCH_HIT_MARGIN;
        float halfHeight = Math.max(entity.getCollisionRadius(), Math.abs(entity.getScaleY()) * SPRITE_HALF_EXTENT)
                + TOUCH_HIT_MARGIN;

        return Math.abs(localX) <= halfWidth && Math.abs(localY) <= halfHeight;
    }

    private static boolean rendersAbove(SimpleWorldState.EntityState entity,
                                        SimpleWorldState.EntityState selectedEntity) {
        if (selectedEntity == null) {
            return true;
        }
        if (entity.getLayer() != selectedEntity.getLayer()) {
            return entity.getLayer() > selectedEntity.getLayer();
        }
        if (entity.getRenderOrder() != selectedEntity.getRenderOrder()) {
            return entity.getRenderOrder() > selectedEntity.getRenderOrder();
        }
        if (entity.getZ() != selectedEntity.getZ()) {
            return entity.getZ() > selectedEntity.getZ();
        }
        return true;
    }

    private void applyQueuedWorldCommands(SimpleWorldState worldState) {
        Integer entityId;
        while ((entityId = worldInteractionController.pollButtonPressRequest()) != null) {
            SimpleWorldState.EntityState entity = worldState.findEntityById(entityId);
            if (entity == null) {
                continue;
            }
            entity.setAnimationState(BUTTON_PRESS_ANIMATION_START_STATE);
        }

        while ((entityId = worldInteractionController.pollReverseDirectionRequest()) != null) {
            SimpleWorldState.EntityState entity = worldState.findEntityById(entityId);
            if (entity == null) {
                continue;
            }
            entity.setVelocityX(-entity.getVelocityX());
            entity.setVelocityY(-entity.getVelocityY());
        }
    }
}
