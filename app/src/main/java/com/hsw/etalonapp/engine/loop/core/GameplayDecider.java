package com.hsw.etalonapp.engine.loop.core;

public interface GameplayDecider {
    /**
     * Runs deterministic AI/gameplay decisions before physics integration.
     */
    WorldState decideGameplay(FrameContext frameContext, WorldState currentWorldState);
}


