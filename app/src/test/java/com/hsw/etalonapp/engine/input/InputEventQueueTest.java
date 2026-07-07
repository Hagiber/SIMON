package com.hsw.etalonapp.engine.input;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.hsw.etalonapp.engine.api.TouchInputEvent;
import com.hsw.etalonapp.engine.loop.core.FrameContext;
import com.hsw.etalonapp.engine.loop.core.InputSnapshot;

import org.junit.Test;

import java.util.List;

public class InputEventQueueTest {

    @Test
    public void drain_returnsQueuedEventsInOrderAndClearsQueue() {
        InputEventQueue inputEventQueue = new InputEventQueue();
        TouchInputEvent downEvent = new TouchInputEvent(TouchInputEvent.Action.DOWN,
                3,
                10.0f,
                20.0f,
                100,
                200,
                1L);
        TouchInputEvent moveEvent = new TouchInputEvent(TouchInputEvent.Action.MOVE,
                3,
                12.0f,
                25.0f,
                100,
                200,
                2L);

        inputEventQueue.push(downEvent);
        inputEventQueue.push(moveEvent);

        List<TouchInputEvent> drainedEvents = inputEventQueue.drain();

        assertEquals(2, drainedEvents.size());
        assertEquals(downEvent, drainedEvents.get(0));
        assertEquals(moveEvent, drainedEvents.get(1));
        assertTrue(inputEventQueue.drain().isEmpty());
    }

    @Test
    public void queuedInputReader_drainsQueueIntoTouchSnapshotAtUpdateReadTime() {
        InputEventQueue inputEventQueue = new InputEventQueue();
        QueuedInputReader inputReader = new QueuedInputReader(inputEventQueue);
        TouchInputEvent event = new TouchInputEvent(TouchInputEvent.Action.DOWN,
                1,
                50.0f,
                60.0f,
                100,
                100,
                1L);

        inputEventQueue.push(event);

        InputSnapshot inputSnapshot = inputReader.read(new FrameContext(7, 0.016f));
        TouchInputSnapshot touchInputSnapshot = (TouchInputSnapshot) inputSnapshot;

        assertEquals(1, touchInputSnapshot.getEvents().size());
        assertEquals(event, touchInputSnapshot.getEvents().get(0));
        assertTrue(((TouchInputSnapshot) inputReader.read(new FrameContext(8, 0.016f))).isEmpty());
    }
}
