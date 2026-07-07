package com.hsw.simonapp.engine.loop.core;

public interface InputApplier {
    /**
     * Applies the tick-local input snapshot before gameplay decisions run.
     */
    WorldState applyInput(FrameContext frameContext, WorldState currentWorldState, InputSnapshot inputSnapshot);
}


