package com.hsw.simonapp.engine.loop.defaults;

final class PhysicsSystem {

    private final WorldBounds worldBounds;

    PhysicsSystem() {
        this(WorldBounds.defaults());
    }

    PhysicsSystem(WorldBounds worldBounds) {
        this.worldBounds = worldBounds;
    }

    void integrate(SimpleWorldState worldState, float deltaSeconds) {
        for (SimpleWorldState.EntityState entity : worldState.getEntities()) {
            integrateEntityPhysics(entity, deltaSeconds);
        }
    }

    private void integrateEntityPhysics(SimpleWorldState.EntityState entity, float deltaSeconds) {
        float nextX = entity.getX() + entity.getVelocityX() * deltaSeconds;
        float nextY = entity.getY() + entity.getVelocityY() * deltaSeconds;

        if (worldBounds.exceedsHorizontalCenterLimit(nextX, entity.getCollisionRadius())) {
            entity.setVelocityX(-entity.getVelocityX());
            nextX = worldBounds.clampHorizontalCenter(nextX, entity.getCollisionRadius());
        }

        if (worldBounds.exceedsVerticalCenterLimit(nextY, entity.getCollisionRadius())) {
            entity.setVelocityY(-entity.getVelocityY());
            nextY = worldBounds.clampVerticalCenter(nextY, entity.getCollisionRadius());
        }

        entity.setX(nextX);
        entity.setY(nextY);
        entity.setRotationDeg(entity.getRotationDeg() + entity.getAngularVelocityDeg() * deltaSeconds);
        entity.setAnimationState(nextAnimationState(entity, deltaSeconds));
    }

    private static float nextAnimationState(SimpleWorldState.EntityState entity, float deltaSeconds) {
        float nextAnimationState = entity.getAnimationState() + entity.getAnimationSpeed() * deltaSeconds;
        if (entity.getAnimationSpeed() < 0.0f) {
            return Math.max(0.0f, nextAnimationState);
        }
        return nextAnimationState;
    }
}
