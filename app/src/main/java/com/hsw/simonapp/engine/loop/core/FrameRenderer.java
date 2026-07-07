package com.hsw.simonapp.engine.loop.core;

public interface FrameRenderer {
    void render(FrameContext frameContext, RenderFrameState renderFrameState);

    default void render(FrameContext frameContext,
                        RenderFrameState renderFrameState,
                        FrameTimingAccumulator timingAccumulator) {
        render(frameContext, renderFrameState);
    }
}


