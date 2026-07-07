package com.hsw.etalonapp.engine.api;

public final class OrthoCamera {

    private static final OrthoCamera DEFAULT = new OrthoCamera(0.0f, 0.0f, 1.0f, 0.0f);

    private final float x;
    private final float y;
    private final float zoom;
    private final float rotationDeg;

    private OrthoCamera(float x, float y, float zoom, float rotationDeg) {
        if (!Float.isFinite(x) || !Float.isFinite(y) ||
                !Float.isFinite(zoom) || !Float.isFinite(rotationDeg)) {
            throw new IllegalArgumentException("OrthoCamera values must be finite");
        }
        if (zoom <= 0.0f) {
            throw new IllegalArgumentException("zoom must be > 0");
        }
        this.x = x;
        this.y = y;
        this.zoom = zoom;
        this.rotationDeg = rotationDeg;
    }

    public static OrthoCamera defaults() {
        return DEFAULT;
    }

    public static OrthoCamera of(float x, float y, float zoom, float rotationDeg) {
        if (x == 0.0f && y == 0.0f && zoom == 1.0f && rotationDeg == 0.0f) {
            return DEFAULT;
        }
        return new OrthoCamera(x, y, zoom, rotationDeg);
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public float getZoom() {
        return zoom;
    }

    public float getRotationDeg() {
        return rotationDeg;
    }
}
