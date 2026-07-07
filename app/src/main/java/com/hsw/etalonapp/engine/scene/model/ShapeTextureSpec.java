package com.hsw.etalonapp.engine.scene.model;

import java.util.Objects;

public final class ShapeTextureSpec {
    private final String debugName;
    private final ShapeKind kind;
    private final ShapeFillMode fillMode;
    private final RgbColor color;
    private final int width;
    private final int height;
    private final int strokeWidth;

    private ShapeTextureSpec(String debugName,
                             ShapeKind kind,
                             ShapeFillMode fillMode,
                             RgbColor color,
                             int width,
                             int height,
                             int strokeWidth) {
        this.debugName = requireDebugName(debugName);
        this.kind = Objects.requireNonNull(kind, "kind");
        this.fillMode = Objects.requireNonNull(fillMode, "fillMode");
        this.color = Objects.requireNonNull(color, "color");
        this.width = requirePositive("width", width);
        this.height = requirePositive("height", height);
        this.strokeWidth = requirePositive("strokeWidth", strokeWidth);
    }

    public static ShapeTextureSpec line(String debugName, int width, int thickness, RgbColor color) {
        return new ShapeTextureSpec(debugName,
                ShapeKind.LINE,
                ShapeFillMode.FILLED,
                color,
                width,
                thickness,
                thickness);
    }

    public static ShapeTextureSpec ellipse(String debugName,
                                           int width,
                                           int height,
                                           ShapeFillMode fillMode,
                                           int strokeWidth,
                                           RgbColor color) {
        return new ShapeTextureSpec(debugName,
                ShapeKind.ELLIPSE,
                fillMode,
                color,
                width,
                height,
                strokeWidth);
    }

    public static ShapeTextureSpec square(String debugName,
                                          int size,
                                          ShapeFillMode fillMode,
                                          int strokeWidth,
                                          RgbColor color) {
        return new ShapeTextureSpec(debugName,
                ShapeKind.SQUARE,
                fillMode,
                color,
                size,
                size,
                strokeWidth);
    }

    public String getDebugName() {
        return debugName;
    }

    public ShapeKind getKind() {
        return kind;
    }

    public ShapeFillMode getFillMode() {
        return fillMode;
    }

    public RgbColor getColor() {
        return color;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getStrokeWidth() {
        return strokeWidth;
    }

    private static String requireDebugName(String debugName) {
        String value = Objects.requireNonNull(debugName, "debugName").trim();
        if (value.isEmpty()) {
            throw new IllegalArgumentException("debugName must not be blank");
        }
        return value;
    }

    private static int requirePositive(String name, int value) {
        if (value <= 0) {
            throw new IllegalArgumentException(name + " must be positive, got: " + value);
        }
        return value;
    }
}
