package com.hsw.simonapp.engine.audio;

public interface AudioSubsystem {
    void consumePendingEvents();

    static AudioSubsystem noOp() {
        return () -> {
        };
    }
}
