package com.hsw.etalonapp.engine.loop.core;

import java.util.List;

public interface UpdateEventEmitter {
    /**
     * Emits deterministic update-side intent after collision resolution.
     * Implementations may publish audio, VFX, log, analytics, or similar events,
     * but must not mutate authoritative world state.
     */
    void emit(FrameContext frameContext, WorldState currentWorldState, List<CollisionPair> collisions);

    static UpdateEventEmitter ignoring() {
        return (frameContext, currentWorldState, collisions) -> {
        };
    }
}


