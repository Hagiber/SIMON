package com.hsw.simonapp.engine.loop.core;

import java.util.Objects;

public final class WorldStateBuffer {

    private WorldState previousWorldState;
    private WorldState currentWorldState;

    public WorldStateBuffer(WorldState initialWorldState) {
        WorldState nonNullInitialState = Objects.requireNonNull(initialWorldState, "initialWorldState");
        this.previousWorldState = nonNullInitialState;
        this.currentWorldState = nonNullInitialState;
    }

    public void advanceTo(WorldState nextCurrentWorldState) {
        previousWorldState = currentWorldState;
        currentWorldState = Objects.requireNonNull(nextCurrentWorldState, "nextCurrentWorldState");
    }

    public void resetTo(WorldState worldState) {
        WorldState nonNullWorldState = Objects.requireNonNull(worldState, "worldState");
        previousWorldState = nonNullWorldState;
        currentWorldState = nonNullWorldState;
    }

    public WorldState getCurrentWorldState() {
        return currentWorldState;
    }

    public RenderFrameState toRenderFrameState(float interpolationAlpha) {
        return new RenderFrameState(previousWorldState, currentWorldState, interpolationAlpha);
    }
}


