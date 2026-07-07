package com.hsw.etalonapp.engine.loop.core;

public final class CollisionPair {
    private final int entityA;
    private final int entityB;

    public CollisionPair(int entityA, int entityB) {
        this.entityA = entityA;
        this.entityB = entityB;
    }

    public int getEntityA() {
        return entityA;
    }

    public int getEntityB() {
        return entityB;
    }
}


