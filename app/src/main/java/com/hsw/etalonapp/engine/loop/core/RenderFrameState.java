package com.hsw.etalonapp.engine.loop.core;

import java.util.Objects;

public final class RenderFrameState {

    private final WorldState previousWorldState;
    private final WorldState currentWorldState;
    private final float interpolationAlpha;

    RenderFrameState(WorldState previousWorldState, WorldState currentWorldState, float interpolationAlpha) {
        this.previousWorldState = Objects.requireNonNull(previousWorldState, "previousWorldState");
        this.currentWorldState = Objects.requireNonNull(currentWorldState, "currentWorldState");
        if (Float.isNaN(interpolationAlpha) || interpolationAlpha < 0.0f || interpolationAlpha > 1.0f) {
            throw new IllegalArgumentException("interpolationAlpha must be in [0, 1]");
        }
        this.interpolationAlpha = interpolationAlpha;
    }

    public WorldState getPreviousWorldState() {
        return previousWorldState;
    }

    public WorldState getCurrentWorldState() {
        return currentWorldState;
    }

    public float getInterpolationAlpha() {
        return interpolationAlpha;
    }
}


