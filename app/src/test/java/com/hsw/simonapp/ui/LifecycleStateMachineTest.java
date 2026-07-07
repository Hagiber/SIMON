package com.hsw.simonapp.ui;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class LifecycleStateMachineTest {

    @Test
    public void shouldOwnRenderer_requiresResumeAndSizedSurface() {
        LifecycleStateMachine stateMachine = new LifecycleStateMachine();

        stateMachine.onResumed();
        assertFalse(stateMachine.shouldOwnRenderer());

        stateMachine.onSurfaceAvailable(0, 1080);
        assertFalse(stateMachine.shouldOwnRenderer());

        stateMachine.onSurfaceAvailable(1920, 1080);
        assertTrue(stateMachine.shouldOwnRenderer());

        stateMachine.onPaused();
        assertFalse(stateMachine.shouldOwnRenderer());
    }

    @Test
    public void shouldRender_requiresInitializedRenderer() {
        LifecycleStateMachine stateMachine = new LifecycleStateMachine();

        stateMachine.onResumed();
        stateMachine.onSurfaceAvailable(1920, 1080);
        assertFalse(stateMachine.shouldRender());

        stateMachine.onRendererInitialized(true);
        assertTrue(stateMachine.shouldRender());

        stateMachine.onSurfaceDestroyed();
        assertFalse(stateMachine.shouldRender());
    }

    @Test
    public void surfaceDestroyed_clearsInitializedRendererOwnership() {
        LifecycleStateMachine stateMachine = new LifecycleStateMachine();

        stateMachine.onResumed();
        stateMachine.onSurfaceAvailable(1920, 1080);
        stateMachine.onRendererInitialized(true);

        stateMachine.onSurfaceDestroyed();
        stateMachine.onSurfaceAvailable(1920, 1080);

        assertTrue(stateMachine.shouldOwnRenderer());
        assertFalse(stateMachine.shouldRender());
    }
}
