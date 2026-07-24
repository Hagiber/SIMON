package com.hsw.simonapp.engine.scene.layout;

import java.util.Objects;

public final class SpriteLayoutRule {
    private final int textureSlot;
    private final FitMode fitMode;
    private final float fillRatio;
    private final float scaleXMultiplier;
    private final float scaleYMultiplier;

    public SpriteLayoutRule(int textureSlot, FitMode fitMode, float fillRatio) {
        this(textureSlot, fitMode, fillRatio, 1.0f, 1.0f);
    }

    public SpriteLayoutRule(int textureSlot,
                            FitMode fitMode,
                            float fillRatio,
                            float scaleXMultiplier,
                            float scaleYMultiplier) {
        if (textureSlot < 0) {
            throw new IllegalArgumentException("textureSlot must be non-negative");
        }
        if (!Float.isFinite(fillRatio) || fillRatio <= 0.0f) {
            throw new IllegalArgumentException("fillRatio must be positive and finite");
        }
        if (!Float.isFinite(scaleXMultiplier) || scaleXMultiplier <= 0.0f) {
            throw new IllegalArgumentException("scaleXMultiplier must be positive and finite");
        }
        if (!Float.isFinite(scaleYMultiplier) || scaleYMultiplier <= 0.0f) {
            throw new IllegalArgumentException("scaleYMultiplier must be positive and finite");
        }
        this.textureSlot = textureSlot;
        this.fitMode = Objects.requireNonNull(fitMode, "fitMode");
        this.fillRatio = fillRatio;
        this.scaleXMultiplier = scaleXMultiplier;
        this.scaleYMultiplier = scaleYMultiplier;
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

    public float getScaleXMultiplier() {
        return scaleXMultiplier;
    }

    public float getScaleYMultiplier() {
        return scaleYMultiplier;
    }
}
