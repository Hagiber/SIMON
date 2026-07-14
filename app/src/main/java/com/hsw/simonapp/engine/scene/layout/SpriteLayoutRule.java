package com.hsw.simonapp.engine.scene.layout;

import java.util.Objects;

public final class SpriteLayoutRule {
    private final int textureSlot;
    private final FitMode fitMode;
    private final float fillRatio;

    public SpriteLayoutRule(int textureSlot, FitMode fitMode, float fillRatio) {
        if (textureSlot < 0) {
            throw new IllegalArgumentException("textureSlot must be non-negative");
        }
        if (!Float.isFinite(fillRatio) || fillRatio <= 0.0f) {
            throw new IllegalArgumentException("fillRatio must be positive and finite");
        }
        this.textureSlot = textureSlot;
        this.fitMode = Objects.requireNonNull(fitMode, "fitMode");
        this.fillRatio = fillRatio;
    }

    public int getTextureSlot() {
        return textureSlot;
    }

    public FitMode getFitMode() {
        return fitMode;
    }

    public float getFillRatio() {
        return fillRatio;
    }
}
