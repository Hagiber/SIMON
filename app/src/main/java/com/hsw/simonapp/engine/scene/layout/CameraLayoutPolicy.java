package com.hsw.simonapp.engine.scene.layout;

import com.hsw.simonapp.engine.api.OrthoCamera;

import java.util.Objects;

public final class CameraLayoutPolicy {
    private final float referenceWidth;
    private final float referenceHeight;
    private final FitMode fitMode;
    private final float zoomMultiplier;

    public CameraLayoutPolicy(float referenceWidth,
                              float referenceHeight,
                              FitMode fitMode) {
        this(referenceWidth, referenceHeight, fitMode, 1.0f);
    }

    public CameraLayoutPolicy(float referenceWidth,
                              float referenceHeight,
                              FitMode fitMode,
                              float zoomMultiplier) {
        if (!Float.isFinite(referenceWidth) || referenceWidth <= 0.0f ||
                !Float.isFinite(referenceHeight) || referenceHeight <= 0.0f) {
            throw new IllegalArgumentException("Reference camera dimensions must be positive and finite");
        }
        if (!Float.isFinite(zoomMultiplier) || zoomMultiplier <= 0.0f) {
            throw new IllegalArgumentException("zoomMultiplier must be positive and finite");
        }
        this.referenceWidth = referenceWidth;
        this.referenceHeight = referenceHeight;
        this.fitMode = Objects.requireNonNull(fitMode, "fitMode");
        this.zoomMultiplier = zoomMultiplier;
    }

    public OrthoCamera cameraForViewport(int viewportWidth, int viewportHeight) {
        ViewportWorldMetrics metrics = ViewportWorldMetrics.fromViewport(viewportWidth, viewportHeight);
        float baseWidth = metrics.getBaseVisibleWorldWidth();
        float baseHeight = metrics.getBaseVisibleWorldHeight();
        float referenceShortSide = Math.min(referenceWidth, referenceHeight);

        float zoom;
        switch (fitMode) {
            case FIT_SHORT_SIDE:
                zoom = Math.min(baseWidth, baseHeight) / referenceShortSide;
                break;
            case FIT_WIDTH:
                zoom = baseWidth / referenceWidth;
                break;
            case FIT_HEIGHT:
                zoom = baseHeight / referenceHeight;
                break;
            case CONTAIN:
                zoom = Math.min(baseWidth / referenceWidth, baseHeight / referenceHeight);
                break;
            case COVER:
                zoom = Math.max(baseWidth / referenceWidth, baseHeight / referenceHeight);
                break;
            default:
                throw new IllegalArgumentException("Unsupported fit mode: " + fitMode);
        }

        return OrthoCamera.of(0.0f, 0.0f, zoom * zoomMultiplier, 0.0f);
    }
}
