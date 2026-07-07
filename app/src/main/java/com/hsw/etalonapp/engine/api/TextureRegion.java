package com.hsw.etalonapp.engine.api;

public final class TextureRegion {

    private static final float EPSILON = 0.0001f;
    private static final TextureRegion FULL = new TextureRegion(0.0f, 0.0f, 1.0f, 1.0f);

    private final float u;
    private final float v;
    private final float widthUv;
    private final float heightUv;

    public TextureRegion(float u, float v, float widthUv, float heightUv) {
        if (!isFinite(u) || !isFinite(v) || !isFinite(widthUv) || !isFinite(heightUv)) {
            throw new IllegalArgumentException("TextureRegion values must be finite");
        }
        if (u < 0.0f || v < 0.0f || widthUv <= 0.0f || heightUv <= 0.0f ||
                u + widthUv > 1.0f + EPSILON ||
                v + heightUv > 1.0f + EPSILON) {
            throw new IllegalArgumentException("TextureRegion must fit inside normalized texture coordinates");
        }
        this.u = clamp01(u);
        this.v = clamp01(v);
        this.widthUv = Math.min(widthUv, 1.0f - this.u);
        this.heightUv = Math.min(heightUv, 1.0f - this.v);
    }

    public static TextureRegion full() {
        return FULL;
    }

    public float getU() {
        return u;
    }

    public float getV() {
        return v;
    }

    public float getWidthUv() {
        return widthUv;
    }

    public float getHeightUv() {
        return heightUv;
    }

    private static boolean isFinite(float value) {
        return !Float.isNaN(value) && !Float.isInfinite(value);
    }

    private static float clamp01(float value) {
        return Math.max(0.0f, Math.min(1.0f, value));
    }
}
