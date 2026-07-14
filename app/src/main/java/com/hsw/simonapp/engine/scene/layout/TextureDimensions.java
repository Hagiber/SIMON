package com.hsw.simonapp.engine.scene.layout;

public final class TextureDimensions {
    private final int width;
    private final int height;

    private TextureDimensions(int width, int height) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Texture dimensions must be positive");
        }
        this.width = width;
        this.height = height;
    }

    public static TextureDimensions of(int width, int height) {
        return new TextureDimensions(width, height);
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }
}
