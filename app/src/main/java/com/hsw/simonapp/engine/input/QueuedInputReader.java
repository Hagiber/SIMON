package com.hsw.simonapp.engine.input;

import com.hsw.simonapp.engine.api.TouchInputEvent;
import com.hsw.simonapp.engine.loop.core.FrameContext;
import com.hsw.simonapp.engine.loop.core.InputReader;
import com.hsw.simonapp.engine.loop.core.InputSnapshot;

import java.util.List;
import java.util.Objects;

public final class QueuedInputReader implements InputReader {

    private final InputEventQueue inputEventQueue;

    public QueuedInputReader(InputEventQueue inputEventQueue) {
        this.inputEventQueue = Objects.requireNonNull(inputEventQueue, "inputEventQueue");
    }

    @Override
    public InputSnapshot read(FrameContext frameContext) {
        List<TouchInputEvent> drainedEvents = inputEventQueue.drain();
        if (drainedEvents.isEmpty()) {
            return TouchInputSnapshot.EMPTY;
        }
        return new TouchInputSnapshot(drainedEvents);
    }
}
