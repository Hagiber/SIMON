package com.hsw.etalonapp.engine.render;

import android.util.Log;

import com.hsw.etalonapp.engine.loop.core.FrameTimingSink;

import java.util.Locale;
import java.util.concurrent.atomic.AtomicLong;

final class FrameDiagnostics implements FrameTimingSink, RenderLoopController.MailboxDropListener {

    private static final String TAG = "EngineFrameTiming";
    private static final int SUMMARY_INTERVAL_FRAMES = 60;
    private static final long SLOW_FRAME_NANOS = 18_000_000L;

    private final AtomicLong mailboxOverwriteCount = new AtomicLong();

    private long intervalFrameCount;
    private long intervalUpdateNanos;
    private long intervalAudioNanos;
    private long intervalSnapshotBuildNanos;
    private long intervalNativeRenderNanos;
    private long intervalRenderTotalNanos;
    private long intervalFrameTotalNanos;
    private long intervalMaxFrameTotalNanos;
    private long intervalMaxNativeRenderNanos;
    private long lastLoggedMailboxOverwriteCount;

    @Override
    public void onMailboxOverwrite(long totalOverwriteCount) {
        mailboxOverwriteCount.set(totalOverwriteCount);
    }

    @Override
    public synchronized void recordFrameTiming(long frameIndex,
                                               float deltaSeconds,
                                               int updateTicks,
                                               long updateNanos,
                                               long audioNanos,
                                               long snapshotBuildNanos,
                                               long nativeRenderNanos,
                                               long renderTotalNanos,
                                               long frameTotalNanos) {
        intervalFrameCount++;
        intervalUpdateNanos += updateNanos;
        intervalAudioNanos += audioNanos;
        intervalSnapshotBuildNanos += snapshotBuildNanos;
        intervalNativeRenderNanos += nativeRenderNanos;
        intervalRenderTotalNanos += renderTotalNanos;
        intervalFrameTotalNanos += frameTotalNanos;
        intervalMaxFrameTotalNanos = Math.max(intervalMaxFrameTotalNanos, frameTotalNanos);
        intervalMaxNativeRenderNanos = Math.max(intervalMaxNativeRenderNanos, nativeRenderNanos);

        boolean slowFrame = frameTotalNanos >= SLOW_FRAME_NANOS;
        boolean catchUpFrame = updateTicks > 1;
        if (slowFrame || catchUpFrame) {
            logFrame(slowFrame ? "slow" : "catch-up",
                    frameIndex,
                    deltaSeconds,
                    updateTicks,
                    updateNanos,
                    audioNanos,
                    snapshotBuildNanos,
                    nativeRenderNanos,
                    renderTotalNanos,
                    frameTotalNanos,
                    true);
        }

        if (intervalFrameCount >= SUMMARY_INTERVAL_FRAMES) {
            logSummary(frameIndex);
            resetInterval();
        }
    }

    private void logFrame(String reason,
                          long frameIndex,
                          float deltaSeconds,
                          int updateTicks,
                          long updateNanos,
                          long audioNanos,
                          long snapshotBuildNanos,
                          long nativeRenderNanos,
                          long renderTotalNanos,
                          long frameTotalNanos,
                          boolean warning) {
        long mailboxOverwrites = mailboxOverwriteCount.get();
        long mailboxDelta = mailboxOverwrites - lastLoggedMailboxOverwriteCount;
        lastLoggedMailboxOverwriteCount = mailboxOverwrites;

        String message = String.format(Locale.US,
                "%s frame=%d dt=%.2fms ticks=%d total=%.2fms update=%.2fms audio=%.2fms snapshot=%.2fms native=%.2fms render=%.2fms mailboxDropDelta=%d mailboxDropTotal=%d",
                reason,
                frameIndex,
                deltaSeconds * 1000.0f,
                updateTicks,
                millis(frameTotalNanos),
                millis(updateNanos),
                millis(audioNanos),
                millis(snapshotBuildNanos),
                millis(nativeRenderNanos),
                millis(renderTotalNanos),
                mailboxDelta,
                mailboxOverwrites);
        if (warning) {
            Log.w(TAG, message);
        } else {
            Log.i(TAG, message);
        }
    }

    private void logSummary(long frameIndex) {
        long mailboxOverwrites = mailboxOverwriteCount.get();
        long mailboxDelta = mailboxOverwrites - lastLoggedMailboxOverwriteCount;
        lastLoggedMailboxOverwriteCount = mailboxOverwrites;

        Log.i(TAG, String.format(Locale.US,
                "summary frame=%d frames=%d avgTotal=%.2fms maxTotal=%.2fms avgUpdate=%.2fms avgAudio=%.2fms avgSnapshot=%.2fms avgNative=%.2fms maxNative=%.2fms avgRender=%.2fms mailboxDropDelta=%d mailboxDropTotal=%d",
                frameIndex,
                intervalFrameCount,
                averageMillis(intervalFrameTotalNanos),
                millis(intervalMaxFrameTotalNanos),
                averageMillis(intervalUpdateNanos),
                averageMillis(intervalAudioNanos),
                averageMillis(intervalSnapshotBuildNanos),
                averageMillis(intervalNativeRenderNanos),
                millis(intervalMaxNativeRenderNanos),
                averageMillis(intervalRenderTotalNanos),
                mailboxDelta,
                mailboxOverwrites));
    }

    private void resetInterval() {
        intervalFrameCount = 0L;
        intervalUpdateNanos = 0L;
        intervalAudioNanos = 0L;
        intervalSnapshotBuildNanos = 0L;
        intervalNativeRenderNanos = 0L;
        intervalRenderTotalNanos = 0L;
        intervalFrameTotalNanos = 0L;
        intervalMaxFrameTotalNanos = 0L;
        intervalMaxNativeRenderNanos = 0L;
    }

    private double averageMillis(long nanos) {
        if (intervalFrameCount == 0L) {
            return 0.0d;
        }
        return millis(nanos) / intervalFrameCount;
    }

    private static double millis(long nanos) {
        return nanos / 1_000_000.0d;
    }
}
