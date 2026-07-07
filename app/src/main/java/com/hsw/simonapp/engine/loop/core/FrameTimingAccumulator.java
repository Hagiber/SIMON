package com.hsw.simonapp.engine.loop.core;

public final class FrameTimingAccumulator {

    private long snapshotBuildNanos;
    private long nativeRenderNanos;

    public void addSnapshotBuildNanos(long nanos) {
        snapshotBuildNanos += Math.max(0L, nanos);
    }

    public void addNativeRenderNanos(long nanos) {
        nativeRenderNanos += Math.max(0L, nanos);
    }

    public long getSnapshotBuildNanos() {
        return snapshotBuildNanos;
    }

    public long getNativeRenderNanos() {
        return nativeRenderNanos;
    }
}
