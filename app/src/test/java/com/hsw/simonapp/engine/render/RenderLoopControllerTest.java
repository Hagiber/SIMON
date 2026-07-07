package com.hsw.simonapp.engine.render;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import com.hsw.simonapp.engine.loop.core.FrameContext;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class RenderLoopControllerTest {

    @Test
    public void start_runsFramesUntilStopped() throws Exception {
        CountDownLatch firstFrame = new CountDownLatch(1);
        AtomicInteger frameCount = new AtomicInteger();
        RenderLoopController controller = new RenderLoopController(frameContext -> {
            frameCount.incrementAndGet();
            firstFrame.countDown();
        }, new ThreadedTestFrameScheduler());

        controller.start();
        try {
            assertTrue(firstFrame.await(1, TimeUnit.SECONDS));
            assertTrue(controller.isRunning());
        } finally {
            controller.stop();
        }

        int frameCountAfterStop = frameCount.get();
        Thread.sleep(25L);

        assertFalse(controller.isRunning());
        assertEquals(frameCountAfterStop, frameCount.get());
    }

    @Test
    public void start_doesNotSpawnSecondConsumerWhileAlreadyRunning() throws Exception {
        CountDownLatch firstFrame = new CountDownLatch(1);
        Set<Long> consumerThreadIds = ConcurrentHashMap.newKeySet();
        RenderLoopController controller = new RenderLoopController(frameContext -> {
            consumerThreadIds.add(Thread.currentThread().getId());
            firstFrame.countDown();
        }, new ThreadedTestFrameScheduler());

        controller.start();
        assertTrue(firstFrame.await(1, TimeUnit.SECONDS));

        controller.start();
        Thread.sleep(25L);
        controller.stop();

        assertEquals(1, consumerThreadIds.size());
    }

    @Test
    public void start_afterStopRestartsLoop() throws Exception {
        CountDownLatch firstRun = new CountDownLatch(1);
        CountDownLatch secondRun = new CountDownLatch(1);
        AtomicInteger frameCount = new AtomicInteger();
        RenderLoopController controller = new RenderLoopController(frameContext -> {
            if (frameCount.incrementAndGet() == 1) {
                firstRun.countDown();
            } else {
                secondRun.countDown();
            }
        }, new ThreadedTestFrameScheduler());

        controller.start();
        assertTrue(firstRun.await(1, TimeUnit.SECONDS));
        controller.stop();

        controller.start();
        try {
            assertTrue(secondRun.await(1, TimeUnit.SECONDS));
            assertTrue(controller.isRunning());
        } finally {
            controller.stop();
        }
    }

    @Test
    public void stop_fromRenderConsumerDoesNotWaitOnItself() throws Exception {
        CountDownLatch stopRequested = new CountDownLatch(1);
        RenderLoopController[] controllerRef = new RenderLoopController[1];
        controllerRef[0] = new RenderLoopController(frameContext -> {
            controllerRef[0].stop();
            stopRequested.countDown();
        }, new ThreadedTestFrameScheduler());

        controllerRef[0].start();

        assertTrue(stopRequested.await(1, TimeUnit.SECONDS));
        Thread.sleep(25L);
        assertFalse(controllerRef[0].isRunning());
    }

    @Test
    public void vsyncProducerAndRenderConsumerRunOnDifferentThreads() throws Exception {
        ThreadedTestFrameScheduler scheduler = new ThreadedTestFrameScheduler();
        CountDownLatch frameConsumed = new CountDownLatch(1);
        AtomicLong consumerThreadId = new AtomicLong(-1L);
        RenderLoopController controller = new RenderLoopController(frameContext -> {
            consumerThreadId.set(Thread.currentThread().getId());
            frameConsumed.countDown();
        }, scheduler);

        controller.start();
        try {
            assertTrue(frameConsumed.await(1, TimeUnit.SECONDS));
        } finally {
            controller.stop();
        }

        assertNotEquals(-1L, scheduler.getProducerThreadId());
        assertNotEquals(scheduler.getProducerThreadId(), consumerThreadId.get());
    }

    @Test
    public void slowConsumerDropsIntermediateVsyncsAndUsesLatestTimestamp() throws Exception {
        ManualFrameScheduler scheduler = new ManualFrameScheduler();
        CountDownLatch firstFrameStarted = new CountDownLatch(1);
        CountDownLatch releaseFirstFrame = new CountDownLatch(1);
        CountDownLatch secondFrameConsumed = new CountDownLatch(1);
        AtomicLong mailboxDrops = new AtomicLong();
        List<FrameContext> consumedFrames = Collections.synchronizedList(new ArrayList<>());
        RenderLoopController controller = new RenderLoopController(frameContext -> {
            consumedFrames.add(frameContext);
            if (consumedFrames.size() == 1) {
                firstFrameStarted.countDown();
                await(releaseFirstFrame);
            } else {
                secondFrameConsumed.countDown();
            }
        }, scheduler, mailboxDrops::set);

        controller.start();
        try {
            scheduler.dispatchFrame(1_000_000_000L);
            assertTrue(firstFrameStarted.await(1, TimeUnit.SECONDS));

            scheduler.dispatchFrame(1_016_666_667L);
            scheduler.dispatchFrame(1_033_333_334L);
            scheduler.dispatchFrame(1_050_000_000L);
            releaseFirstFrame.countDown();

            assertTrue(secondFrameConsumed.await(1, TimeUnit.SECONDS));
        } finally {
            controller.stop();
        }

        assertEquals(2, consumedFrames.size());
        assertEquals(0L, consumedFrames.get(0).getFrameIndex());
        assertEquals(3L, consumedFrames.get(1).getFrameIndex());
        assertEquals(0.05f, consumedFrames.get(1).getDeltaSeconds(), 0.0001f);
        assertEquals(2L, mailboxDrops.get());
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
        }
    }

    private static final class ManualFrameScheduler implements RenderLoopController.FrameScheduler {
        private RenderLoopController.VsyncFrameCallback callback;

        @Override
        public synchronized void start(RenderLoopController.VsyncFrameCallback callback) {
            this.callback = callback;
        }

        @Override
        public synchronized void stop() {
            callback = null;
        }

        private boolean dispatchFrame(long frameTimeNanos) {
            RenderLoopController.VsyncFrameCallback currentCallback;
            synchronized (this) {
                currentCallback = callback;
            }
            return currentCallback != null && currentCallback.doFrame(frameTimeNanos);
        }
    }

    private static final class ThreadedTestFrameScheduler implements RenderLoopController.FrameScheduler {
        private static final long TEST_FRAME_NANOS = 16_666_667L;

        private volatile boolean running;
        private volatile long producerThreadId = -1L;
        private Thread producerThread;

        @Override
        public synchronized void start(RenderLoopController.VsyncFrameCallback callback) {
            if (running) {
                return;
            }
            running = true;
            producerThread = new Thread(() -> runFrames(callback), "TestVsyncProducerThread");
            producerThread.start();
        }

        @Override
        public void stop() {
            Thread thread;
            synchronized (this) {
                running = false;
                thread = producerThread;
                producerThread = null;
            }
            if (thread != null && thread != Thread.currentThread()) {
                thread.interrupt();
                try {
                    thread.join();
                } catch (InterruptedException interruptedException) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        private long getProducerThreadId() {
            return producerThreadId;
        }

        private void runFrames(RenderLoopController.VsyncFrameCallback callback) {
            producerThreadId = Thread.currentThread().getId();
            long frameTimeNanos = System.nanoTime();
            try {
                while (running) {
                    if (!callback.doFrame(frameTimeNanos)) {
                        return;
                    }
                    frameTimeNanos += TEST_FRAME_NANOS;
                    Thread.sleep(1L);
                }
            } catch (InterruptedException interruptedException) {
                Thread.currentThread().interrupt();
            } finally {
                running = false;
            }
        }
    }
}
