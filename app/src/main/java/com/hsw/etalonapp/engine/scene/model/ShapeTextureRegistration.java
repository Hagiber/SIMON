package com.hsw.etalonapp.engine.scene.model;

import java.util.Objects;

public final class ShapeTextureRegistration {
    private final int textureSlot;
    private final ShapeTextureSpec textureSpec;

    public ShapeTextureRegistration(int textureSlot, ShapeTextureSpec textureSpec) {
        if (textureSlot < 0) {
            throw new IllegalArgumentException("textureSlot must be non-negative, got: " + textureSlot);
        }
        this.textureSlot = textureSlot;
        this.textureSpec = Objects.requireNonNull(textureSpec, "textureSpec");
    }

    public int getTextureSlot() {
        return textureSlot;
    }

    public ShapeTextureSpec getTextureSpec() {
        return textureSpec;
    }
}
