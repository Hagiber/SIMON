package com.hsw.etalonapp.engine.loop.core;

public final class FrameContext {
    private final long frameIndex;
    private final float deltaSeconds;

    public FrameContext(long frameIndex, float deltaSeconds) {
        this.frameIndex = frameIndex;
        this.deltaSeconds = deltaSeconds;
    }

    public long getFrameIndex() {
        return frameIndex;
    }

    public float getDeltaSeconds() {
        return deltaSeconds;
    }
}


