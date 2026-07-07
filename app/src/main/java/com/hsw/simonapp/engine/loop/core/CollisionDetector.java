package com.hsw.simonapp.engine.loop.core;

import java.util.List;

public interface CollisionDetector {
    List<CollisionPair> detect(FrameContext frameContext, WorldState worldState);
}


