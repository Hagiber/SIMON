package com.hsw.simonapp.engine.audio;

import java.util.Objects;

public final class AudioEvent {

    private final AudioEventType type;
    private final long sourceTickIndex;
    private final String dedupeKey;
    private final String throttleKey;
    private final int toneIndex;

    private AudioEvent(AudioEventType type,
                       long sourceTickIndex,
                       String dedupeKey,
                       String throttleKey,
                       int toneIndex) {
        if (sourceTickIndex < 0L) {
            throw new IllegalArgumentException("sourceTickIndex must be >= 0, got: " + sourceTickIndex);
        }
        this.type = Objects.requireNonNull(type, "type");
        this.sourceTickIndex = sourceTickIndex;
        this.dedupeKey = requireNonEmpty(dedupeKey, "dedupeKey");
        this.throttleKey = requireNonEmpty(throttleKey, "throttleKey");
        this.toneIndex = toneIndex;
    }

    public static AudioEvent collisionSound(long sourceTickIndex, int entityA, int entityB) {
        int firstEntity = Math.min(entityA, entityB);
        int secondEntity = Math.max(entityA, entityB);
        AudioEventType eventType = AudioEventType.COLLISION_SOUND;
        return new AudioEvent(eventType,
                sourceTickIndex,
                eventType.getEventName() + ":" + firstEntity + ":" + secondEntity,
                eventType.getEventName(),
                -1);
    }

    public static AudioEvent buttonTone(long sourceTickIndex, int entityId, int toneIndex) {
        if (toneIndex < 0) {
            throw new IllegalArgumentException("toneIndex must be >= 0, got: " + toneIndex);
        }
        AudioEventType eventType = AudioEventType.BUTTON_TONE;
        return new AudioEvent(eventType,
                sourceTickIndex,
                eventType.getEventName() + ":" + entityId,
                eventType.getEventName() + ":" + toneIndex,
                toneIndex);
    }

    public AudioEventType getType() {
        return type;
    }

    public long getSourceTickIndex() {
        return sourceTickIndex;
    }

    public String getDedupeKey() {
        return dedupeKey;
    }

    public String getThrottleKey() {
        return throttleKey;
    }

    public int getToneIndex() {
        return toneIndex;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof AudioEvent)) {
            return false;
        }
        AudioEvent that = (AudioEvent) other;
        return sourceTickIndex == that.sourceTickIndex
                && toneIndex == that.toneIndex
                && type == that.type
                && dedupeKey.equals(that.dedupeKey)
                && throttleKey.equals(that.throttleKey);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, sourceTickIndex, dedupeKey, throttleKey, toneIndex);
    }

    @Override
    public String toString() {
        return "AudioEvent{"
                + "type=" + type
                + ", sourceTickIndex=" + sourceTickIndex
                + ", dedupeKey='" + dedupeKey + '\''
                + ", throttleKey='" + throttleKey + '\''
                + ", toneIndex=" + toneIndex
                + '}';
    }

    private static String requireNonEmpty(String value, String name) {
        if (value == null || value.isEmpty()) {
            throw new IllegalArgumentException(name + " must not be empty");
        }
        return value;
    }
}
