package com.hsw.etalonapp.engine.audio;

public interface AudioSubsystem {
    void consumePendingEvents();

    static AudioSubsystem noOp() {
        return () -> {
        };
    }
}
