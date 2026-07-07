package com.hsw.simonapp.engine.scene.model;

public final class RgbColor {
    private final int red;
    private final int green;
    private final int blue;

    private RgbColor(int red, int green, int blue) {
        this.red = red;
        this.green = green;
        this.blue = blue;
    }

    public static RgbColor of(int red, int green, int blue) {
        validateChannel("red", red);
        validateChannel("green", green);
        validateChannel("blue", blue);
        return new RgbColor(red, green, blue);
    }

    public int getRed() {
        return red;
    }

    public int getGreen() {
        return green;
    }

    public int getBlue() {
        return blue;
    }

    private static void validateChannel(String name, int value) {
        if (value < 0 || value > 255) {
            throw new IllegalArgumentException(name + " RGB channel must be in [0, 255], got: " + value);
        }
    }
}
