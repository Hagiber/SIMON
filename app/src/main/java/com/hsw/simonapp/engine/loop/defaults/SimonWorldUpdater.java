package com.hsw.simonapp.engine.loop.defaults;

import com.hsw.simonapp.engine.api.OrthoCamera;
import com.hsw.simonapp.engine.audio.AudioEvent;
import com.hsw.simonapp.engine.audio.AudioEventSink;
import com.hsw.simonapp.engine.loop.core.FrameContext;
import com.hsw.simonapp.engine.loop.core.GameplayDecider;
import com.hsw.simonapp.engine.loop.core.InputApplier;
import com.hsw.simonapp.engine.loop.core.InputSnapshot;
import com.hsw.simonapp.engine.loop.core.PhysicsIntegrator;
import com.hsw.simonapp.engine.loop.core.WorldState;
import com.hsw.simonapp.engine.api.TouchInputEvent;
import com.hsw.simonapp.engine.gameplay.SimonGameplayController;
import com.hsw.simonapp.engine.gameplay.SimonGameplayController.InputResult;
import com.hsw.simonapp.engine.gameplay.SimonGameplayController.SimonButton;
import com.hsw.simonapp.engine.gameplay.SimonGameplayController.SimonGameplayEvent;
import com.hsw.simonapp.engine.input.TouchInputSnapshot;
import com.hsw.simonapp.engine.scene.TextureCatalog;

import java.util.Objects;

public final class SimonWorldUpdater implements InputApplier, GameplayDecider, PhysicsIntegrator {

    private static final float SPRITE_HALF_EXTENT = 0.35f;
    private static final float TOUCH_HIT_MARGIN = 0.04f;
    private static final float DEGREES_TO_RADIANS = 0.0174532925199432957f;
    private static final float BUTTON_PRESS_ANIMATION_START_STATE = 1.0f;

    private final TouchInputStateReducer touchInputStateReducer;
    private final GameplaySystem gameplaySystem;
    private final PhysicsSystem physicsSystem;
    private final WorldBounds worldBounds;
    private final WorldInteractionController worldInteractionController;
    private final AudioEventSink audioEventSink;
    private final SimonGameplayController simonGameplayController;

    public SimonWorldUpdater() {
        this(new WorldInteractionController());
    }

    public SimonWorldUpdater(WorldInteractionController worldInteractionController) {
        this(worldInteractionController, AudioEventSink.ignoring());
    }

    public SimonWorldUpdater(WorldInteractionController worldInteractionController,
                             AudioEventSink audioEventSink) {
        this(WorldBounds.defaults(),
                worldInteractionController,
                audioEventSink,
                new SimonGameplayController());
    }

    SimonWorldUpdater(WorldInteractionController worldInteractionController,
                      AudioEventSink audioEventSink,
                      SimonGameplayController simonGameplayController) {
        this(WorldBounds.defaults(), worldInteractionController, audioEventSink, simonGameplayController);
    }

    private SimonWorldUpdater(WorldBounds worldBounds,
                              WorldInteractionController worldInteractionController,
                              AudioEventSink audioEventSink,
                              SimonGameplayController simonGameplayController) {
        this(new TouchInputStateReducer(worldBounds),
                new GameplaySystem(worldBounds),
                new PhysicsSystem(worldBounds),
                worldBounds,
                worldInteractionController,
                audioEventSink,
                simonGameplayController);
    }

    SimonWorldUpdater(TouchInputStateReducer touchInputStateReducer,
                      GameplaySystem gameplaySystem,
                      PhysicsSystem physicsSystem) {
        this(touchInputStateReducer,
                gameplaySystem,
                physicsSystem,
                WorldBounds.defaults(),
                new WorldInteractionController(),
                AudioEventSink.ignoring(),
                new SimonGameplayController());
    }

    private SimonWorldUpdater(TouchInputStateReducer touchInputStateReducer,
                              GameplaySystem gameplaySystem,
                              PhysicsSystem physicsSystem,
                              WorldBounds worldBounds,
                              WorldInteractionController worldInteractionController,
                              AudioEventSink audioEventSink,
                              SimonGameplayController simonGameplayController) {
        this.touchInputStateReducer = touchInputStateReducer;
        this.gameplaySystem = gameplaySystem;
        this.physicsSystem = physicsSystem;
        this.worldBounds = worldBounds;
        this.worldInteractionController = Objects.requireNonNull(worldInteractionController,
                "worldInteractionController");
        this.audioEventSink = Objects.requireNonNull(audioEventSink, "audioEventSink");
        this.simonGameplayController = Objects.requireNonNull(simonGameplayController,
                "simonGameplayController");
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
        applyTouchSelections(frameContext, inputSnapshot, nextWorldState);
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
        applySimonGameplay(frameContext, nextWorldState);
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

    private void applyTouchSelections(FrameContext frameContext,
                                      InputSnapshot inputSnapshot,
                                      SimpleWorldState worldState) {
        if (!(inputSnapshot instanceof TouchInputSnapshot)) {
            return;
        }

        TouchInputSnapshot touchInputSnapshot = (TouchInputSnapshot) inputSnapshot;
        for (TouchInputEvent event : touchInputSnapshot.getEvents()) {
            if (event.getAction() == TouchInputEvent.Action.DOWN) {
                selectTouchedEntity(frameContext, event, worldState);
            }
        }
    }

    private void selectTouchedEntity(FrameContext frameContext,
                                     TouchInputEvent event,
                                     SimpleWorldState worldState) {
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
                    handleTouchedSimonButton(selectedEntity);
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

    private void publishButtonTone(FrameContext frameContext, SimpleWorldState.EntityState selectedEntity) {
        int textureSlot = selectedEntity.getTextureSlot();
        if (!TextureCatalog.isButtonTextureSlot(textureSlot)) {
            return;
        }

        int toneIndex = TextureCatalog.buttonColorIndexForSlot(textureSlot);
        audioEventSink.publish(AudioEvent.buttonTone(frameContext.getFrameIndex(),
                selectedEntity.getEntityId(),
                toneIndex));
    }

    private void handleTouchedSimonButton(SimpleWorldState.EntityState selectedEntity) {
        SimonButton button = simonButtonForTextureSlot(selectedEntity.getTextureSlot());
        if (button == null) {
            return;
        }

        InputResult ignored = simonGameplayController.handlePlayerButton(button);
        if (ignored == InputResult.IGNORED) {
            return;
        }
    }

    private void applySimonGameplay(FrameContext frameContext, SimpleWorldState worldState) {
        while (worldInteractionController.pollStartGameRequest() != null) {
            simonGameplayController.start();
        }

        simonGameplayController.advance(frameContext.getDeltaSeconds());

        SimonGameplayEvent event;
        while ((event = simonGameplayController.pollEvent()) != null) {
            applySimonGameplayEvent(frameContext, worldState, event);
        }
    }

    private void applySimonGameplayEvent(FrameContext frameContext,
                                         SimpleWorldState worldState,
                                         SimonGameplayEvent event) {
        switch (event.getType()) {
            case SHOW_BUTTON:
            case PLAYER_INPUT_ACCEPTED:
            case PLAYER_INPUT_REJECTED:
                pressSimonButton(frameContext, worldState, event.getButton());
                break;
            case ROUND_COMPLETED:
                worldInteractionController.notifyScore(event.getScore());
                break;
            case GAME_OVER:
                worldInteractionController.notifyGameOver(event.getScore());
                break;
            default:
                break;
        }
    }

    private void pressSimonButton(FrameContext frameContext,
                                  SimpleWorldState worldState,
                                  SimonButton button) {
        SimpleWorldState.EntityState entity = findSimonButtonEntity(worldState, button);
        if (entity == null) {
            return;
        }

        worldInteractionController.requestButtonPress(entity.getEntityId());
        publishButtonTone(frameContext, entity);
    }

    private static SimpleWorldState.EntityState findSimonButtonEntity(SimpleWorldState worldState,
                                                                      SimonButton button) {
        int textureSlot = textureSlotForButton(button);
        for (SimpleWorldState.EntityState entity : worldState.getEntities()) {
            if (entity.getTextureSlot() == textureSlot) {
                return entity;
            }
        }
        return null;
    }

    private static SimonButton simonButtonForTextureSlot(int textureSlot) {
        if (textureSlot == TextureCatalog.RED_BUTTON_TEXTURE_SLOT) {
            return SimonButton.RED;
        }
        if (textureSlot == TextureCatalog.GREEN_BUTTON_TEXTURE_SLOT) {
            return SimonButton.GREEN;
        }
        if (textureSlot == TextureCatalog.BLUE_BUTTON_TEXTURE_SLOT) {
            return SimonButton.BLUE;
        }
        if (textureSlot == TextureCatalog.YELLOW_BUTTON_TEXTURE_SLOT) {
            return SimonButton.YELLOW;
        }
        return null;
    }

    private static int textureSlotForButton(SimonButton button) {
        switch (Objects.requireNonNull(button, "button")) {
            case RED:
                return TextureCatalog.RED_BUTTON_TEXTURE_SLOT;
            case GREEN:
                return TextureCatalog.GREEN_BUTTON_TEXTURE_SLOT;
            case BLUE:
                return TextureCatalog.BLUE_BUTTON_TEXTURE_SLOT;
            case YELLOW:
                return TextureCatalog.YELLOW_BUTTON_TEXTURE_SLOT;
            default:
                throw new IllegalArgumentException("Unsupported Simon button: " + button);
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
