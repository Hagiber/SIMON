package com.hsw.vulkansmokehost;

import android.os.Handler;
import android.os.HandlerThread;
import android.view.Choreographer;

import java.util.Objects;

final class SmokeRenderLoopController {

    private static final String VSYNC_PRODUCER_THREAD_NAME = "SmokeVsyncProducerThread";
    private static final String RENDER_CONSUMER_THREAD_NAME = "SmokeRenderConsumerThread";

    private final FrameRunner frameRunner;
    private final FrameScheduler frameScheduler;
    private final LatestFrameMailbox frameMailbox = new LatestFrameMailbox();

    private volatile boolean running;
    private long frameIndex;
    private Thread renderConsumerThread;

    SmokeRenderLoopController(FrameRunner frameRunner) {
        this(frameRunner, new ChoreographerFrameScheduler());
    }

    SmokeRenderLoopController(FrameRunner frameRunner, FrameScheduler frameScheduler) {
        this.frameRunner = Objects.requireNonNull(frameRunner, "frameRunner");
        this.frameScheduler = Objects.requireNonNull(frameScheduler, "frameScheduler");
    }

    void start() {
        Thread consumerThread;
        synchronized (this) {
            if (running) {
                return;
            }
            running = true;
            frameMailbox.open();
            consumerThread = new Thread(this::consumeFrames, RENDER_CONSUMER_THREAD_NAME);
            renderConsumerThread = consumerThread;
        }

        consumerThread.start();
        try {
            frameScheduler.start(this::publishVsync);
        } catch (RuntimeException | Error exception) {
            stop();
            throw exception;
        }
    }

    void stop() {
        Thread consumerThread;
        synchronized (this) {
            if (!running && renderConsumerThread == null) {
                return;
            }
            running = false;
            consumerThread = renderConsumerThread;
        }

        frameScheduler.stop();
        frameMailbox.close();
        if (consumerThread != null && consumerThread != Thread.currentThread()) {
            consumerThread.interrupt();
            try {
                consumerThread.join();
            } catch (InterruptedException interruptedException) {
                Thread.currentThread().interrupt();
            }
        }

        synchronized (this) {
            if (renderConsumerThread == consumerThread &&
                    (consumerThread == null || !consumerThread.isAlive())) {
                renderConsumerThread = null;
            }
        }
    }

    synchronized boolean isRunning() {
        return running;
    }

    private boolean publishVsync(long frameTimeNanos) {
        long publishedFrameIndex;
        synchronized (this) {
            if (!running) {
                return false;
            }
            publishedFrameIndex = frameIndex++;
        }

        frameMailbox.publish(new VsyncFrame(publishedFrameIndex, frameTimeNanos));
        return running;
    }

    private void consumeFrames() {
        long previousConsumedFrameTimeNanos = 0L;
        boolean stopScheduler = false;
        try {
            while (running) {
                VsyncFrame vsyncFrame = frameMailbox.awaitLatest();
                if (vsyncFrame == null) {
                    return;
                }

                float deltaSeconds = calculateDeltaSeconds(previousConsumedFrameTimeNanos,
                        vsyncFrame.frameTimeNanos);
                previousConsumedFrameTimeNanos = vsyncFrame.frameTimeNanos;
                if (!frameRunner.runFrame(new FrameContext(vsyncFrame.frameIndex,
                        vsyncFrame.frameTimeNanos,
                        deltaSeconds))) {
                    return;
                }
            }
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
        } finally {
            synchronized (this) {
                stopScheduler = running;
                running = false;
                if (Thread.currentThread() == renderConsumerThread) {
                    renderConsumerThread = null;
                }
            }
            frameMailbox.close();
            if (stopScheduler) {
                frameScheduler.stop();
            }
        }
    }

    private static float calculateDeltaSeconds(long previousFrameTimeNanos, long frameTimeNanos) {
        if (previousFrameTimeNanos <= 0L) {
            return 0.0f;
        }
        long deltaNanos = Math.max(0L, frameTimeNanos - previousFrameTimeNanos);
        return deltaNanos / 1_000_000_000f;
    }

    interface FrameRunner {
        boolean runFrame(FrameContext frameContext);
    }

    interface FrameScheduler {
        void start(VsyncFrameCallback callback);

        void stop();
    }

    interface VsyncFrameCallback {
        boolean doFrame(long frameTimeNanos);
    }

    static final class FrameContext {
        private final long frameIndex;
        private final long frameTimeNanos;
        private final float deltaSeconds;

        private FrameContext(long frameIndex, long frameTimeNanos, float deltaSeconds) {
            this.frameIndex = frameIndex;
            this.frameTimeNanos = frameTimeNanos;
            this.deltaSeconds = deltaSeconds;
        }

        long getFrameIndex() {
            return frameIndex;
        }

        long getFrameTimeNanos() {
            return frameTimeNanos;
        }

        float getDeltaSeconds() {
            return deltaSeconds;
        }
    }

    private static final class LatestFrameMailbox {
        private VsyncFrame latestFrame;
        private boolean open;

        synchronized void open() {
            latestFrame = null;
            open = true;
        }

        synchronized void publish(VsyncFrame frame) {
            if (!open) {
                return;
            }
            latestFrame = frame;
            notifyAll();
        }

        synchronized VsyncFrame awaitLatest() throws InterruptedException {
            while (open && latestFrame == null) {
                wait();
            }
            if (!open) {
                return null;
            }

            VsyncFrame frame = latestFrame;
            latestFrame = null;
            return frame;
        }

        synchronized void close() {
            open = false;
            latestFrame = null;
            notifyAll();
        }
    }

    private static final class VsyncFrame {
        private final long frameIndex;
        private final long frameTimeNanos;

        private VsyncFrame(long frameIndex, long frameTimeNanos) {
            this.frameIndex = frameIndex;
            this.frameTimeNanos = frameTimeNanos;
        }
    }

    private static final class ChoreographerFrameScheduler implements FrameScheduler {
        private boolean active;
        private HandlerThread producerThread;
        private Handler producerHandler;
        private Choreographer choreographer;
        private Choreographer.FrameCallback frameCallback;

        @Override
        public synchronized void start(VsyncFrameCallback callback) {
            if (active) {
                return;
            }

            active = true;
            producerThread = new HandlerThread(VSYNC_PRODUCER_THREAD_NAME);
            producerThread.start();
            producerHandler = new Handler(producerThread.getLooper());
            Handler localHandler = producerHandler;
            localHandler.post(() -> startOnProducerThread(localHandler, callback));
        }

        @Override
        public void stop() {
            HandlerThread threadToStop;
            Handler handlerToStop;
            Choreographer choreographerToStop;
            Choreographer.FrameCallback callbackToRemove;
            synchronized (this) {
                if (!active && producerThread == null) {
                    return;
                }
                active = false;
                threadToStop = producerThread;
                handlerToStop = producerHandler;
                choreographerToStop = choreographer;
                callbackToRemove = frameCallback;
                producerThread = null;
                producerHandler = null;
                choreographer = null;
                frameCallback = null;
            }

            if (handlerToStop != null && choreographerToStop != null && callbackToRemove != null) {
                handlerToStop.post(() -> choreographerToStop.removeFrameCallback(callbackToRemove));
            }
            if (threadToStop != null) {
                threadToStop.quitSafely();
                if (threadToStop != Thread.currentThread()) {
                    try {
                        threadToStop.join();
                    } catch (InterruptedException interruptedException) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }

        private void startOnProducerThread(Handler localHandler, VsyncFrameCallback callback) {
            if (!isActive()) {
                return;
            }

            Choreographer localChoreographer = Choreographer.getInstance();
            Choreographer.FrameCallback localFrameCallback = new Choreographer.FrameCallback() {
                @Override
                public void doFrame(long frameTimeNanos) {
                    if (!isActive()) {
                        return;
                    }
                    if (callback.doFrame(frameTimeNanos) && isActive()) {
                        localChoreographer.postFrameCallback(this);
                    }
                }
            };

            synchronized (this) {
                if (!active || producerHandler != localHandler) {
                    return;
                }
                choreographer = localChoreographer;
                frameCallback = localFrameCallback;
            }
            localChoreographer.postFrameCallback(localFrameCallback);
        }

        private synchronized boolean isActive() {
            return active;
        }
    }
}
