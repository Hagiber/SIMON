package com.hsw.etalonapp.engine.audio;

public interface AudioPlayer {
    void play(AudioEvent event);

    static AudioPlayer noOp() {
        return event -> {
        };
    }
}
