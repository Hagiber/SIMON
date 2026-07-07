package com.hsw.etalonapp.engine.loop.core;

public interface InputReader {
    /** Called at the start of each fixed update tick to produce the tick-local input snapshot. */
    InputSnapshot read(FrameContext frameContext);
}


