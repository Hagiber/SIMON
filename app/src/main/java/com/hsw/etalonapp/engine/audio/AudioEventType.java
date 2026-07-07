package com.hsw.etalonapp.engine.audio;

public enum AudioEventType {
    COLLISION_SOUND("collision_sound");

    private final String eventName;

    AudioEventType(String eventName) {
        this.eventName = eventName;
    }

    public String getEventName() {
        return eventName;
    }
}
