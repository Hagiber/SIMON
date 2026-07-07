package com.hsw.simonapp.engine.input;

import com.hsw.simonapp.engine.api.TouchInputEvent;
import com.hsw.simonapp.engine.loop.core.InputSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class TouchInputSnapshot implements InputSnapshot {

    public static final TouchInputSnapshot EMPTY = new TouchInputSnapshot(Collections.emptyList());

    private final List<TouchInputEvent> events;

    public TouchInputSnapshot(List<TouchInputEvent> events) {
        this.events = Collections.unmodifiableList(new ArrayList<>(events));
    }

    public List<TouchInputEvent> getEvents() {
        return events;
    }

    public boolean isEmpty() {
        return events.isEmpty();
    }
}
