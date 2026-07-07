package com.hsw.simonapp.engine.api;

public final class ScissorRect {

    private static final ScissorRect DISABLED = new ScissorRect(0, 0, 0, 0, false);

    private final int x;
    private final int y;
    private final int width;
    private final int height;
    private final boolean enabled;

    private ScissorRect(int x, int y, int width, int height, boolean enabled) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.enabled = enabled;
    }

    public static ScissorRect disabled() {
        return DISABLED;
    }

    public static ScissorRect of(int x, int y, int width, int height) {
        if (x < 0 || y < 0) {
            throw new IllegalArgumentException("scissor origin must be non-negative");
        }
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("scissor size must be positive");
        }
        if ((long) x + width > Integer.MAX_VALUE || (long) y + height > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("scissor bounds exceed integer range");
        }
        return new ScissorRect(x, y, width, height, true);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }
}
