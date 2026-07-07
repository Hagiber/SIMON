package com.hsw.etalonapp.engine.loop.core;

public interface PhysicsIntegrator {
    /**
     * Integrates movement and simulation values before collision detection/resolution.
     */
    WorldState integratePhysics(FrameContext frameContext, WorldState currentWorldState);
}


