package com.hsw.simonapp.engine.audio;

public interface AudioPlayer {
    void play(AudioEvent event);

    static AudioPlayer noOp() {
        return event -> {
        };
    }
}
