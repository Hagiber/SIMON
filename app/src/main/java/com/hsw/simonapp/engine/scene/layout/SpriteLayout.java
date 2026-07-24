package com.hsw.simonapp.engine.scene.layout;

public final class SpriteLayout {
    private final float scaleX;
    private final float scaleY;

    private SpriteLayout(float scaleX, float scaleY) {
        if (!Float.isFinite(scaleX) || !Float.isFinite(scaleY)) {
            throw new IllegalArgumentException("Sprite layout scale values must be finite");
        }
        if (scaleX <= 0.0f || scaleY <= 0.0f) {
            throw new IllegalArgumentException("Sprite layout scale values must be positive");
        }
        this.scaleX = scaleX;
        this.scaleY = scaleY;
    }

    public static SpriteLayout uniform(float scale) {
        return new SpriteLayout(scale, scale);
    }

    public SpriteLayout multiply(float scaleXMultiplier, float scaleYMultiplier) {
        return new SpriteLayout(scaleX * scaleXMultiplier, scaleY * scaleYMultiplier);
    }

    public float getScaleX() {
        return scaleX;
    }

    public float getScaleY() {
        return scaleY;
    }
}
