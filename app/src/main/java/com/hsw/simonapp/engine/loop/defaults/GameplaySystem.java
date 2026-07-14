package com.hsw.simonapp.engine.loop.defaults;

final class GameplaySystem {

    private static final float HUMAN_TOUCH_MOVE_SPEED = 2.7f;
    private static final float TOUCH_TARGET_EPSILON = 0.01f;

    private final WorldBounds worldBounds;

    GameplaySystem() {
        this(WorldBounds.defaults());
    }

    GameplaySystem(WorldBounds worldBounds) {
        this.worldBounds = worldBounds;
    }

    void apply(SimpleWorldState worldState, Float activeTouchTargetX, Float activeTouchTargetY) {
        for (SimpleWorldState.EntityState entity : worldState.getEntities()) {
            applyHumanTouchInput(entity, activeTouchTargetX, activeTouchTargetY);
        }
    }

    private void applyHumanTouchInput(SimpleWorldState.EntityState entity,
                                      Float activeTouchTargetX,
                                      Float activeTouchTargetY) {
        if (entity.getControlMode() != SimpleWorldState.EntityState.ControlMode.HUMAN_TOUCH
                || entity.getTouchInteraction() != SimpleWorldState.EntityState.TouchInteraction.MOVE_TO_TOUCH
                || activeTouchTargetX == null
                || activeTouchTargetY == null) {
            return;
        }

        float targetX = worldBounds.clampHorizontalCenter(activeTouchTargetX, entity.getCollisionRadius());
        float targetY = worldBounds.clampVerticalCenter(activeTouchTargetY, entity.getCollisionRadius());
        float deltaX = targetX - entity.getX();
        float deltaY = targetY - entity.getY();
        float distance = (float) Math.sqrt(deltaX * deltaX + deltaY * deltaY);
        if (distance <= TOUCH_TARGET_EPSILON) {
            entity.setVelocityX(0.0f);
            entity.setVelocityY(0.0f);
            return;
        }

        entity.setVelocityX(deltaX / distance * HUMAN_TOUCH_MOVE_SPEED);
        entity.setVelocityY(deltaY / distance * HUMAN_TOUCH_MOVE_SPEED);
    }
}
