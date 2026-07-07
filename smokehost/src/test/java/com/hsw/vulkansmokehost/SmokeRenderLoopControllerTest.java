package com.hsw.vulkansmokehost;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class SmokeRenderLoopControllerTest {

    @Test
    public void producerAndConsumerUseDifferentThreads() throws Exception {
        ThreadedTestFrameScheduler scheduler = new ThreadedTestFrameScheduler();
        CountDownLatch frameConsumed = new CountDownLatch(1);
        AtomicLong consumerThreadId = new AtomicLong(-1L);
        SmokeRenderLoopController controller = new SmokeRenderLoopController(frameContext -> {
            consumerThreadId.set(Thread.currentThread().getId());
            frameConsumed.countDown();
            return true;
        }, scheduler);

        controller.start();
        try {
            assertTrue(frameConsumed.await(1, TimeUnit.SECONDS));
        } finally {
            controller.stop();
        }

        assertNotEquals(-1L, scheduler.producerThreadId);
        assertNotEquals(scheduler.producerThreadId, consumerThreadId.get());
    }

    @Test
    public void slowConsumerDropsIntermediateVsyncs() throws Exception {
        ManualFrameScheduler scheduler = new ManualFrameScheduler();
        CountDownLatch firstFrameStarted = new CountDownLatch(1);
        CountDownLatch releaseFirstFrame = new CountDownLatch(1);
        CountDownLatch secondFrameConsumed = new CountDownLatch(1);
        List<SmokeRenderLoopController.FrameContext> frames =
                Collections.synchronizedList(new ArrayList<>());
        SmokeRenderLoopController controller = new SmokeRenderLoopController(frameContext -> {
            frames.add(frameContext);
            if (frames.size() == 1) {
                firstFrameStarted.countDown();
                await(releaseFirstFrame);
            } else {
                secondFrameConsumed.countDown();
            }
            return true;
        }, scheduler);

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

        assertEquals(2, frames.size());
        assertEquals(0L, frames.get(0).getFrameIndex());
        assertEquals(3L, frames.get(1).getFrameIndex());
        assertEquals(0.05f, frames.get(1).getDeltaSeconds(), 0.0001f);
    }

    @Test
    public void falseFrameResultStopsLoop() throws Exception {
        CountDownLatch frameConsumed = new CountDownLatch(1);
        SmokeRenderLoopController controller = new SmokeRenderLoopController(frameContext -> {
            frameConsumed.countDown();
            return false;
        }, new ThreadedTestFrameScheduler());

        controller.start();
        assertTrue(frameConsumed.await(1, TimeUnit.SECONDS));
        Thread.sleep(25L);

        assertFalse(controller.isRunning());
        controller.stop();
    }

    @Test
    public void startWhileRunningKeepsOneConsumerThread() throws Exception {
        ManualFrameScheduler scheduler = new ManualFrameScheduler();
        Set<Long> consumerThreadIds = ConcurrentHashMap.newKeySet();
        CountDownLatch firstFrameConsumed = new CountDownLatch(1);
        CountDownLatch framesConsumed = new CountDownLatch(2);
        SmokeRenderLoopController controller = new SmokeRenderLoopController(frameContext -> {
            consumerThreadIds.add(Thread.currentThread().getId());
            framesConsumed.countDown();
            firstFrameConsumed.countDown();
            return true;
        }, scheduler);

        controller.start();
        try {
            scheduler.dispatchFrame(1_000_000_000L);
            assertTrue(firstFrameConsumed.await(1, TimeUnit.SECONDS));
            controller.start();
            scheduler.dispatchFrame(1_016_666_667L);
            assertTrue(framesConsumed.await(1, TimeUnit.SECONDS));
        } finally {
            controller.stop();
        }

        assertEquals(1, consumerThreadIds.size());
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
        }
    }

    private static final class ManualFrameScheduler
            implements SmokeRenderLoopController.FrameScheduler {
        private SmokeRenderLoopController.VsyncFrameCallback callback;

        @Override
        public synchronized void start(SmokeRenderLoopController.VsyncFrameCallback callback) {
            this.callback = callback;
        }

        @Override
        public synchronized void stop() {
            callback = null;
        }

        private boolean dispatchFrame(long frameTimeNanos) {
            SmokeRenderLoopController.VsyncFrameCallback currentCallback;
            synchronized (this) {
                currentCallback = callback;
            }
            return currentCallback != null && currentCallback.doFrame(frameTimeNanos);
        }
    }

    private static final class ThreadedTestFrameScheduler
            implements SmokeRenderLoopController.FrameScheduler {
        private volatile boolean running;
        private volatile long producerThreadId = -1L;
        private Thread producerThread;

        @Override
        public synchronized void start(SmokeRenderLoopController.VsyncFrameCallback callback) {
            running = true;
            producerThread = new Thread(() -> {
                producerThreadId = Thread.currentThread().getId();
                long frameTimeNanos = System.nanoTime();
                try {
                    while (running && callback.doFrame(frameTimeNanos)) {
                        frameTimeNanos += 16_666_667L;
                        Thread.sleep(1L);
                    }
                } catch (InterruptedException interruptedException) {
                    Thread.currentThread().interrupt();
                } finally {
                    running = false;
                }
            }, "SmokeTestVsyncProducer");
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
    }
}
