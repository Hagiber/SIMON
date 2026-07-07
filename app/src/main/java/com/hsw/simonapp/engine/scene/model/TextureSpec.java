package com.hsw.simonapp.engine.scene.model;

public final class TextureSpec {
    private final String assetName;
    private final float x;
    private final float y;
    private final float z;
    private final float rotationDeg;
    private final float alphaBlend;

    public TextureSpec(String assetName, float x, float y, float z, float rotationDeg, float alphaBlend) {
        this.assetName = assetName;
        this.x = x;
        this.y = y;
        this.z = z;
        this.rotationDeg = rotationDeg;
        this.alphaBlend = alphaBlend;
    }

    public String getAssetName() {
        return assetName;
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public float getZ() {
        return z;
    }

    public float getRotationDeg() {
        return rotationDeg;
    }

    public float getAlphaBlend() {
        return alphaBlend;
    }
}
