package com.hsw.etalonapp.engine.audio;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedQueue;

public final class AudioEventQueue implements AudioEventSink {

    private final ConcurrentLinkedQueue<AudioEvent> pendingEvents = new ConcurrentLinkedQueue<>();

    @Override
    public void publish(AudioEvent event) {
        pendingEvents.add(Objects.requireNonNull(event, "event"));
    }

    public List<AudioEvent> drain() {
        if (pendingEvents.isEmpty()) {
            return Collections.emptyList();
        }

        List<AudioEvent> drainedEvents = new ArrayList<>();
        AudioEvent event;
        while ((event = pendingEvents.poll()) != null) {
            drainedEvents.add(event);
        }
        return drainedEvents;
    }
}
