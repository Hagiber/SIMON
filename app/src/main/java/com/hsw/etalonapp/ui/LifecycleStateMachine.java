package com.hsw.etalonapp.ui;

public final class LifecycleStateMachine {

    private boolean resumed;
    private boolean surfaceAvailable;
    private boolean surfaceSized;
    private boolean rendererInitialized;

    public void onResumed() {
        resumed = true;
    }

    public void onPaused() {
        resumed = false;
    }

    public void onSurfaceAvailable(int width, int height) {
        surfaceAvailable = true;
        surfaceSized = width > 0 && height > 0;
    }

    public void onSurfaceDestroyed() {
        surfaceAvailable = false;
        surfaceSized = false;
        rendererInitialized = false;
    }

    public void onRendererInitialized(boolean initialized) {
        rendererInitialized = initialized;
    }

    public boolean shouldOwnRenderer() {
        return resumed && surfaceAvailable && surfaceSized;
    }

    public boolean shouldRender() {
        return shouldOwnRenderer() && rendererInitialized;
    }
}
