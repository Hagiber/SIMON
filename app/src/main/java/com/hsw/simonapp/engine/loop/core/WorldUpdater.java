package com.hsw.simonapp.engine.loop.core;

public interface WorldUpdater {
    /**
     * Advances the authoritative simulation from the current completed state and returns the next state.
     * Implementations must run input application, AI/gameplay decision, and physics integration in that order.
     * The caller owns collision resolution and event emission after this returns.
     */
    WorldState update(FrameContext frameContext, WorldState currentWorldState, InputSnapshot inputSnapshot);
}


