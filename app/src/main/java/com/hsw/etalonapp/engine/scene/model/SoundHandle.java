package com.hsw.etalonapp.engine.scene.model;

import java.util.Objects;

public final class SoundHandle {
    private final int value;

    private SoundHandle(int value) {
        this.value = value;
    }

    public static SoundHandle of(int value) {
        if (value < 0) {
            throw new IllegalArgumentException("SoundHandle value must be >= 0, got: " + value);
        }
        return new SoundHandle(value);
    }

    public int value() {
        return value;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof SoundHandle)) {
            return false;
        }
        SoundHandle that = (SoundHandle) other;
        return value == that.value;
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return "SoundHandle{" + "value=" + value + '}';
    }
}
