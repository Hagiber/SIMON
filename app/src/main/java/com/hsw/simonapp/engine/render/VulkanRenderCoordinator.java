package com.hsw.simonapp.engine.render;

import android.app.Activity;
import android.content.res.AssetManager;
import android.view.Surface;

import com.hsw.simonapp.engine.api.TouchInputEvent;
import com.hsw.simonapp.engine.game.DefaultGameDefinition;
import com.hsw.simonapp.engine.game.GameDefinition;
import com.hsw.simonapp.engine.game.SceneDefinition;
import com.hsw.simonapp.engine.scene.NativeSceneSnapshotFactory;
import com.hsw.simonapp.engine.loop.core.FrameContext;
import com.hsw.simonapp.engine.api.NativeLib;

import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public final class VulkanRenderCoordinator {

    private final RendererResourceLifecycle rendererLifecycle;
    private final FrameLoopLifecycle frameLoopLifecycle;
    private final EngineGameLoopRuntime engineGameLoopRuntime;

    private CoordinatorResult lastCoordinatorError = CoordinatorResult.success("No error");

    public VulkanRenderCoordinator(NativeLib nativeLib, AssetManager assetManager) {
        this(nativeLib, assetManager, new DefaultGameDefinition());
    }

    public VulkanRenderCoordinator(NativeLib nativeLib,
                                   AssetManager assetManager,
                                   GameDefinition gameDefinition) {
        NativeLib nonNullNativeLib = Objects.requireNonNull(nativeLib, "nativeLib");
        AssetManager nonNullAssetManager = Objects.requireNonNull(assetManager, "assetManager");
        GameDefinition nonNullGameDefinition = Objects.requireNonNull(gameDefinition, "gameDefinition");
        SceneDefinition sceneDefinition = Objects.requireNonNull(
                nonNullGameDefinition.createSceneDefinition(nonNullAssetManager),
                "sceneDefinition");
        BiConsumer<CoordinatorErrorCode, String> failureSink = this::recordCoordinatorFailure;
        FrameDiagnostics frameDiagnostics = new FrameDiagnostics();

        VulkanRendererSession rendererSession = new VulkanRendererSession(nonNullNativeLib,
                nonNullAssetManager,
                sceneDefinition);
        this.rendererLifecycle = new RendererResourceLifecycle(rendererSession);
        this.engineGameLoopRuntime = new EngineGameLoopRuntime(nonNullAssetManager,
                new NativeSceneFrameRenderer(nonNullNativeLib, new NativeSceneSnapshotFactory(), failureSink),
                frameDiagnostics,
                nonNullGameDefinition,
                sceneDefinition);
        this.frameLoopLifecycle = new FrameLoopLifecycle(new RenderLoopController(engineGameLoopRuntime::runFrame,
                frameDiagnostics));
    }

    public CoordinatorResult ensureInitialized(Activity activity, Surface surface) {
        lastCoordinatorError = rendererLifecycle.init(activity, surface);
        return lastCoordinatorError;
    }

    public String getLastVulkanError() {
        return rendererLifecycle.getLastVulkanError();
    }

    public CoordinatorResult getLastCoordinatorError() {
        return lastCoordinatorError;
    }

    public void resize(int width, int height) {
        engineGameLoopRuntime.resizeViewport(width, height);
        rendererLifecycle.resize(width, height);
    }

    public void startRenderLoop() {
        if (!rendererLifecycle.isInitialized()) {
            return;
        }
        frameLoopLifecycle.start();
    }

    public void renderCurrentFrame() {
        renderCurrentFrames(1);
    }

    public void renderCurrentFrames(int frameCount) {
        if (!rendererLifecycle.isInitialized()) {
            return;
        }
        int clampedFrameCount = Math.max(frameCount, 0);
        for (int i = 0; i < clampedFrameCount && rendererLifecycle.isInitialized(); i++) {
            frameLoopLifecycle.tick(new FrameContext(i, 0.0f));
        }
    }

    public void stopRenderLoop() {
        frameLoopLifecycle.stop();
    }

    public void tick(FrameContext frameContext) {
        if (!rendererLifecycle.isInitialized()) {
            return;
        }
        frameLoopLifecycle.tick(frameContext);
    }

    public void stopAndRelease() {
        stopRenderLoop();
        release();
    }

    public void release() {
        stopRenderLoop();
        rendererLifecycle.release();
    }

    public boolean isInitialized() {
        return rendererLifecycle.isInitialized();
    }

    public void queueTouchInput(TouchInputEvent touchInputEvent) {
        engineGameLoopRuntime.queueTouchInput(Objects.requireNonNull(touchInputEvent, "touchInputEvent"));
    }

    public void setSelectedEntityListener(Consumer<Integer> selectedEntityListener) {
        engineGameLoopRuntime.setSelectedEntityListener(selectedEntityListener);
    }

    public void setButtonPressListener(Consumer<Integer> buttonPressListener) {
        engineGameLoopRuntime.setButtonPressListener(buttonPressListener);
    }

    public void setWorldCoordinateTouchListener(BiConsumer<Float, Float> worldCoordinateTouchListener) {
        engineGameLoopRuntime.setWorldCoordinateTouchListener(worldCoordinateTouchListener);
    }

    public boolean reverseSelectedEntityDirection() {
        return engineGameLoopRuntime.requestReverseSelectedEntityDirection();
    }

    public void setButtonToneVolume(float volume) {
        engineGameLoopRuntime.setButtonToneVolume(volume);
    }

    private void recordCoordinatorFailure(CoordinatorErrorCode errorCode, String message) {
        lastCoordinatorError = CoordinatorResult.failure(errorCode, message);
        stopAndRelease();
    }

    public enum CoordinatorErrorCode {
        VULKAN_INIT_FAILED,
        TEXTURE_UPLOAD_FAILED,
        SCENE_FRAME_RENDER_FAILED
    }

    public static final class CoordinatorResult {
        private final boolean success;
        private final CoordinatorErrorCode errorCode;
        private final String message;

        private CoordinatorResult(boolean success, CoordinatorErrorCode errorCode, String message) {
            this.success = success;
            this.errorCode = errorCode;
            this.message = message;
        }

        public static CoordinatorResult success(String message) {
            return new CoordinatorResult(true, null, message);
        }

        public static CoordinatorResult failure(CoordinatorErrorCode errorCode, String message) {
            return new CoordinatorResult(false, errorCode, message);
        }

        public boolean isSuccess() {
            return success;
        }

        public CoordinatorErrorCode getErrorCode() {
            return errorCode;
        }

        public String getMessage() {
            return message;
        }
    }

}
