package com.hsw.etalonapp.engine.loop.core;

import java.util.List;

public interface CollisionResolver {
    void resolve(FrameContext frameContext, WorldState worldState, List<CollisionPair> collisions);
}


