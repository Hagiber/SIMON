package com.hsw.simonapp.engine.loop.defaults;

import com.hsw.simonapp.engine.loop.core.CollisionPair;
import com.hsw.simonapp.engine.loop.core.CollisionResolver;
import com.hsw.simonapp.engine.loop.core.FrameContext;
import com.hsw.simonapp.engine.loop.core.WorldState;

import java.util.List;

public final class SimpleCollisionResolver implements CollisionResolver {

    @Override
    public void resolve(FrameContext frameContext, WorldState worldState, List<CollisionPair> collisions) {
        if (!(worldState instanceof SimpleWorldState)) {
            return;
        }

        SimpleWorldState castedState = (SimpleWorldState) worldState;
        for (CollisionPair collision : collisions) {
            SimpleWorldState.EntityState entityA = castedState.findEntityById(collision.getEntityA());
            SimpleWorldState.EntityState entityB = castedState.findEntityById(collision.getEntityB());
            if (entityA == null || entityB == null) {
                continue;
            }

            float oldVx = entityA.getVelocityX();
            float oldVy = entityA.getVelocityY();
            entityA.setVelocityX(entityB.getVelocityX());
            entityA.setVelocityY(entityB.getVelocityY());
            entityB.setVelocityX(oldVx);
            entityB.setVelocityY(oldVy);
        }
    }
}


