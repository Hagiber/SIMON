package com.hsw.simonapp.engine.audio;

public enum AudioEventType {
    COLLISION_SOUND("collision_sound"),
    BUTTON_TONE("button_tone");

    private final String eventName;

    AudioEventType(String eventName) {
        this.eventName = eventName;
    }

    public String getEventName() {
        return eventName;
    }
}
