package com.hsw.etalonapp.engine.loop.defaults;

final class WorldBounds {

    static final float BOUNDS = 0.95f;

    private volatile float horizontalBound = BOUNDS;
    private volatile float verticalBound = BOUNDS;

    private WorldBounds() {
    }

    static WorldBounds defaults() {
        return new WorldBounds();
    }

    void setViewportSize(int width, int height) {
        if (width <= 0 || height <= 0) {
            return;
        }

        float aspect = (float) width / (float) height;
        if (aspect >= 1.0f) {
            horizontalBound = BOUNDS * aspect;
            verticalBound = BOUNDS;
        } else {
            horizontalBound = BOUNDS;
            verticalBound = BOUNDS / aspect;
        }
    }

    float horizontalCenterLimit(float collisionRadius) {
        return centerLimit(horizontalBound, collisionRadius);
    }

    float verticalCenterLimit(float collisionRadius) {
        return centerLimit(verticalBound, collisionRadius);
    }

    boolean exceedsHorizontalCenterLimit(float value, float collisionRadius) {
        return Math.abs(value) > horizontalCenterLimit(collisionRadius);
    }

    boolean exceedsVerticalCenterLimit(float value, float collisionRadius) {
        return Math.abs(value) > verticalCenterLimit(collisionRadius);
    }

    float clampHorizontalCenter(float value, float collisionRadius) {
        float limit = horizontalCenterLimit(collisionRadius);
        return clamp(value, -limit, limit);
    }

    float clampVerticalCenter(float value, float collisionRadius) {
        float limit = verticalCenterLimit(collisionRadius);
        return clamp(value, -limit, limit);
    }

    float toWorldX(float normalizedScreenX) {
        return clamp(normalizedScreenX * horizontalBound, -horizontalBound, horizontalBound);
    }

    float toWorldY(float normalizedScreenY) {
        return clamp(normalizedScreenY * verticalBound, -verticalBound, verticalBound);
    }

    private static float centerLimit(float bounds, float collisionRadius) {
        return Math.max(0.0f, bounds - Math.max(0.0f, collisionRadius));
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
