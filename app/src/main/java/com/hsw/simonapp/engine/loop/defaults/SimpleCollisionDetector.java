package com.hsw.simonapp.engine.loop.defaults;

import com.hsw.simonapp.engine.loop.core.CollisionDetector;
import com.hsw.simonapp.engine.loop.core.CollisionPair;
import com.hsw.simonapp.engine.loop.core.FrameContext;
import com.hsw.simonapp.engine.loop.core.WorldState;

import java.util.ArrayList;
import java.util.List;

public final class SimpleCollisionDetector implements CollisionDetector {

    @Override
    public List<CollisionPair> detect(FrameContext frameContext, WorldState worldState) {
        if (!(worldState instanceof SimpleWorldState)) {
            return new ArrayList<>();
        }

        List<SimpleWorldState.EntityState> entities = ((SimpleWorldState) worldState).getEntities();
        List<CollisionPair> collisions = new ArrayList<>();
        for (int i = 0; i < entities.size(); i++) {
            SimpleWorldState.EntityState a = entities.get(i);
            for (int j = i + 1; j < entities.size(); j++) {
                SimpleWorldState.EntityState b = entities.get(j);
                float dx = a.getX() - b.getX();
                float dy = a.getY() - b.getY();
                float radius = a.getCollisionRadius() + b.getCollisionRadius();
                if ((dx * dx + dy * dy) <= radius * radius) {
                    collisions.add(new CollisionPair(a.getEntityId(), b.getEntityId()));
                }
            }
        }
        return collisions;
    }
}


