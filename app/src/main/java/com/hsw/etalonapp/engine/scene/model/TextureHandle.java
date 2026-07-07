package com.hsw.etalonapp.engine.scene.model;

import java.util.Objects;

public final class TextureHandle {
    private final int value;

    private TextureHandle(int value) {
        this.value = value;
    }

    public static TextureHandle of(int value) {
        if (value < 0) {
            throw new IllegalArgumentException("TextureHandle value must be >= 0, got: " + value);
        }
        return new TextureHandle(value);
    }

    public int value() {
        return value;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof TextureHandle)) {
            return false;
        }
        TextureHandle that = (TextureHandle) other;
        return value == that.value;
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return "TextureHandle{" + "value=" + value + '}';
    }
}
