package com.hsw.simonapp.engine.input;

import com.hsw.simonapp.engine.api.TouchInputEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedQueue;

public final class InputEventQueue {

    private final ConcurrentLinkedQueue<TouchInputEvent> queuedEvents = new ConcurrentLinkedQueue<>();

    public void push(TouchInputEvent event) {
        queuedEvents.add(Objects.requireNonNull(event, "event"));
    }

    public List<TouchInputEvent> drain() {
        List<TouchInputEvent> drainedEvents = new ArrayList<>();
        TouchInputEvent event;
        while ((event = queuedEvents.poll()) != null) {
            drainedEvents.add(event);
        }
        return drainedEvents;
    }
}
