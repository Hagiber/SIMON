package com.hsw.simonapp.engine.api;

import android.app.Activity;
import android.content.res.AssetManager;
import android.view.Surface;

import com.hsw.simonapp.engine.game.DefaultGameDefinition;
import com.hsw.simonapp.engine.game.GameDefinition;
import com.hsw.simonapp.engine.render.VulkanRenderCoordinator;
import com.hsw.simonapp.engine.render.VulkanRenderCoordinator.CoordinatorResult;

import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Host-facing embedded engine facade.
 *
 * Android host code owns surfaces, lifecycle callbacks, permissions, and input
 * event capture. Rendering, scene updates, asset loading, and audio playback
 * stay behind this facade.
 */
public final class EmbeddedEngine {

    private final VulkanRenderCoordinator renderCoordinator;

    public EmbeddedEngine(AssetManager assetManager) {
        this(assetManager, new DefaultGameDefinition());
    }

    public EmbeddedEngine(AssetManager assetManager, GameDefinition gameDefinition) {
        this(new NativeLib(), assetManager, gameDefinition);
    }

    EmbeddedEngine(NativeLib nativeLib, AssetManager assetManager) {
        this(nativeLib, assetManager, new DefaultGameDefinition());
    }

    EmbeddedEngine(NativeLib nativeLib, AssetManager assetManager, GameDefinition gameDefinition) {
        this.renderCoordinator = new VulkanRenderCoordinator(
                Objects.requireNonNull(nativeLib, "nativeLib"),
                Objects.requireNonNull(assetManager, "assetManager"),
                Objects.requireNonNull(gameDefinition, "gameDefinition"));
    }

    public Result initialize(Activity activity, Surface surface) {
        return Result.from(renderCoordinator.ensureInitialized(
                Objects.requireNonNull(activity, "activity"),
                Objects.requireNonNull(surface, "surface")));
    }

    public void resize(int width, int height) {
        renderCoordinator.resize(width, height);
    }

    public void start() {
        renderCoordinator.startRenderLoop();
    }

    public void renderCurrentFrame() {
        renderCoordinator.renderCurrentFrame();
    }

    public void renderCurrentFrames(int frameCount) {
        renderCoordinator.renderCurrentFrames(frameCount);
    }

    public void stop() {
        renderCoordinator.stopRenderLoop();
    }

    public void stopAndRelease() {
        renderCoordinator.stopAndRelease();
    }

    public boolean isInitialized() {
        return renderCoordinator.isInitialized();
    }

    public void queueTouchInput(TouchInputEvent touchInputEvent) {
        renderCoordinator.queueTouchInput(touchInputEvent);
    }

    public boolean reverseSelectedEntityDirection() {
        return renderCoordinator.reverseSelectedEntityDirection();
    }

    public void setButtonToneVolume(float volume) {
        renderCoordinator.setButtonToneVolume(volume);
    }

    public void setSelectedEntityListener(Consumer<Integer> selectedEntityListener) {
        renderCoordinator.setSelectedEntityListener(selectedEntityListener);
    }

    public void setButtonPressListener(Consumer<Integer> buttonPressListener) {
        renderCoordinator.setButtonPressListener(buttonPressListener);
    }

    public void setWorldCoordinateTouchListener(BiConsumer<Float, Float> worldCoordinateTouchListener) {
        renderCoordinator.setWorldCoordinateTouchListener(worldCoordinateTouchListener);
    }

    public static final class Result {
        private final boolean success;
        private final ErrorCode errorCode;
        private final String message;

        private Result(boolean success, ErrorCode errorCode, String message) {
            this.success = success;
            this.errorCode = errorCode;
            this.message = message;
        }

        public boolean isSuccess() {
            return success;
        }

        public ErrorCode getErrorCode() {
            return errorCode;
        }

        public String getMessage() {
            return message;
        }

        private static Result from(CoordinatorResult coordinatorResult) {
            if (coordinatorResult.isSuccess()) {
                return new Result(true, null, coordinatorResult.getMessage());
            }
            return new Result(false,
                    ErrorCode.from(coordinatorResult.getErrorCode()),
                    coordinatorResult.getMessage());
        }
    }

    public enum ErrorCode {
        RENDERER_INITIALIZATION_FAILED,
        ASSET_UPLOAD_FAILED,
        FRAME_RENDER_FAILED;

        private static ErrorCode from(VulkanRenderCoordinator.CoordinatorErrorCode coordinatorErrorCode) {
            if (coordinatorErrorCode == null) {
                return null;
            }
            switch (coordinatorErrorCode) {
                case VULKAN_INIT_FAILED:
                    return RENDERER_INITIALIZATION_FAILED;
                case TEXTURE_UPLOAD_FAILED:
                    return ASSET_UPLOAD_FAILED;
                case SCENE_FRAME_RENDER_FAILED:
                    return FRAME_RENDER_FAILED;
                default:
                    throw new IllegalArgumentException("Unsupported coordinator error: " + coordinatorErrorCode);
            }
        }
    }
}
