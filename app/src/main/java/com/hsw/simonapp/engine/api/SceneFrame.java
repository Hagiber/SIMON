package com.hsw.simonapp.engine.api;

import java.util.Objects;

public final class SceneFrame {

    private final SceneSnapshot previousSceneSnapshot;
    private final SceneSnapshot currentSceneSnapshot;
    private final OrthoCamera previousCamera;
    private final OrthoCamera currentCamera;
    private final float interpolationAlpha;

    private SceneFrame(SceneSnapshot previousSceneSnapshot,
                       SceneSnapshot currentSceneSnapshot,
                       OrthoCamera previousCamera,
                       OrthoCamera currentCamera,
                       float interpolationAlpha) {
        this.previousSceneSnapshot = Objects.requireNonNull(previousSceneSnapshot, "previousSceneSnapshot");
        this.currentSceneSnapshot = Objects.requireNonNull(currentSceneSnapshot, "currentSceneSnapshot");
        this.previousCamera = Objects.requireNonNull(previousCamera, "previousCamera");
        this.currentCamera = Objects.requireNonNull(currentCamera, "currentCamera");
        if (previousSceneSnapshot.size() != currentSceneSnapshot.size()) {
            throw new IllegalArgumentException("SceneFrame snapshots must have identical sizes");
        }
        if (Float.isNaN(interpolationAlpha) || interpolationAlpha < 0.0f || interpolationAlpha > 1.0f) {
            throw new IllegalArgumentException("interpolationAlpha must be in [0, 1]");
        }
        this.interpolationAlpha = interpolationAlpha;
    }

    public static SceneFrame of(SceneSnapshot previousSceneSnapshot,
                                SceneSnapshot currentSceneSnapshot,
                                float interpolationAlpha) {
        return of(previousSceneSnapshot,
                currentSceneSnapshot,
                OrthoCamera.defaults(),
                OrthoCamera.defaults(),
                interpolationAlpha);
    }

    public static SceneFrame of(SceneSnapshot previousSceneSnapshot,
                                SceneSnapshot currentSceneSnapshot,
                                OrthoCamera previousCamera,
                                OrthoCamera currentCamera,
                                float interpolationAlpha) {
        return new SceneFrame(previousSceneSnapshot,
                currentSceneSnapshot,
                previousCamera,
                currentCamera,
                interpolationAlpha);
    }

    public static SceneFrame empty(float interpolationAlpha) {
        return of(SceneSnapshot.empty(), SceneSnapshot.empty(), interpolationAlpha);
    }

    SceneSnapshot previousSceneSnapshot() {
        return previousSceneSnapshot;
    }

    SceneSnapshot currentSceneSnapshot() {
        return currentSceneSnapshot;
    }

    OrthoCamera previousCamera() {
        return previousCamera;
    }

    OrthoCamera currentCamera() {
        return currentCamera;
    }

    public float getInterpolationAlpha() {
        return interpolationAlpha;
    }
}
