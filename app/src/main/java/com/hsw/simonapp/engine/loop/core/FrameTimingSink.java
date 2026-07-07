package com.hsw.simonapp.engine.loop.core;

public interface FrameTimingSink {

    FrameTimingSink NO_OP = (frameIndex,
                             deltaSeconds,
                             updateTicks,
                             updateNanos,
                             audioNanos,
                             snapshotBuildNanos,
                             nativeRenderNanos,
                             renderTotalNanos,
                             frameTotalNanos) -> {
    };

    void recordFrameTiming(long frameIndex,
                           float deltaSeconds,
                           int updateTicks,
                           long updateNanos,
                           long audioNanos,
                           long snapshotBuildNanos,
                           long nativeRenderNanos,
                           long renderTotalNanos,
                           long frameTotalNanos);
}
