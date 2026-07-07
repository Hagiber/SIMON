package com.hsw.etalonapp.engine.audio;

public interface AudioEventSink {
    void publish(AudioEvent event);

    static AudioEventSink ignoring() {
        return event -> {
        };
    }
}
