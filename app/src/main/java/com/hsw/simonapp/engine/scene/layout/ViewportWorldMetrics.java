package com.hsw.simonapp.engine.scene.layout;

import com.hsw.simonapp.engine.api.OrthoCamera;

import java.util.Objects;

public final class ViewportWorldMetrics {
    private final float baseVisibleWorldWidth;
    private final float baseVisibleWorldHeight;

    private ViewportWorldMetrics(float baseVisibleWorldWidth, float baseVisibleWorldHeight) {
        this.baseVisibleWorldWidth = baseVisibleWorldWidth;
        this.baseVisibleWorldHeight = baseVisibleWorldHeight;
    }

    public static ViewportWorldMetrics fromViewport(int viewportWidth, int viewportHeight) {
        if (viewportWidth <= 0 || viewportHeight <= 0) {
            throw new IllegalArgumentException("Viewport dimensions must be positive");
        }

        float viewportAspect = (float) viewportWidth / (float) viewportHeight;
        float baseVisibleWorldWidth = viewportAspect >= 1.0f ? 2.0f * viewportAspect : 2.0f;
        float baseVisibleWorldHeight = viewportAspect >= 1.0f ? 2.0f : 2.0f / viewportAspect;
        return new ViewportWorldMetrics(baseVisibleWorldWidth, baseVisibleWorldHeight);
    }

    public float visibleWorldWidth(OrthoCamera camera) {
        return baseVisibleWorldWidth / Objects.requireNonNull(camera, "camera").getZoom();
    }

    public float visibleWorldHeight(OrthoCamera camera) {
        return baseVisibleWorldHeight / Objects.requireNonNull(camera, "camera").getZoom();
    }

    public float shortSide(OrthoCamera camera) {
        return Math.min(visibleWorldWidth(camera), visibleWorldHeight(camera));
    }

    float getBaseVisibleWorldWidth() {
        return baseVisibleWorldWidth;
    }

    float getBaseVisibleWorldHeight() {
        return baseVisibleWorldHeight;
    }
}
