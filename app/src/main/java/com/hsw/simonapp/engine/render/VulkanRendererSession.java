package com.hsw.simonapp.engine.render;

import android.app.Activity;
import android.content.res.AssetManager;
import android.view.Surface;

import com.hsw.simonapp.engine.assets.GeneratedShapeTextureLoader;
import com.hsw.simonapp.engine.assets.GeneratedShapeTextureLoader.ShapeTextureUploadResult;
import com.hsw.simonapp.engine.assets.TextureAssetLoader;
import com.hsw.simonapp.engine.assets.TextureAssetLoader.TextureUploadResult;
import com.hsw.simonapp.engine.game.DefaultGameDefinition;
import com.hsw.simonapp.engine.game.SceneDefinition;
import com.hsw.simonapp.engine.scene.model.ShapeTextureRegistration;
import com.hsw.simonapp.engine.scene.model.TextureSpec;
import com.hsw.simonapp.engine.api.NativeLib;

import java.util.Objects;

final class VulkanRendererSession {

    private final NativeLib nativeLib;
    private final AssetManager assetManager;
    private final TextureAssetLoader textureAssetLoader;
    private final GeneratedShapeTextureLoader generatedShapeTextureLoader;
    private final SceneDefinition sceneDefinition;

    private boolean initialized;

    VulkanRendererSession(NativeLib nativeLib, AssetManager assetManager) {
        this(nativeLib,
                assetManager,
                new DefaultGameDefinition().createSceneDefinition(assetManager));
    }

    VulkanRendererSession(NativeLib nativeLib,
                          AssetManager assetManager,
                          SceneDefinition sceneDefinition) {
        this.nativeLib = Objects.requireNonNull(nativeLib, "nativeLib");
        this.assetManager = Objects.requireNonNull(assetManager, "assetManager");
        this.textureAssetLoader = new TextureAssetLoader(assetManager, nativeLib);
        this.generatedShapeTextureLoader = new GeneratedShapeTextureLoader(nativeLib);
        this.sceneDefinition = Objects.requireNonNull(sceneDefinition, "sceneDefinition");
    }

    VulkanRenderCoordinator.CoordinatorResult ensureInitialized(Activity activity, Surface surface) {
        if (initialized) {
            return VulkanRenderCoordinator.CoordinatorResult.success("Vulkan already initialized");
        }

        if (!nativeLib.initVulkan(activity, surface, assetManager)) {
            return VulkanRenderCoordinator.CoordinatorResult.failure(
                    VulkanRenderCoordinator.CoordinatorErrorCode.VULKAN_INIT_FAILED,
                    "Vulkan init failed: " + nativeLib.getLastVulkanError());
        }

        initialized = true;
        VulkanRenderCoordinator.CoordinatorResult texturePreparationResult = prepareSceneTextures();
        if (!texturePreparationResult.isSuccess()) {
            // Keep failed texture bootstraps from leaving a half-initialized renderer behind.
            textureAssetLoader.clearCache();
            nativeLib.cleanupVulkan();
            initialized = false;
            return texturePreparationResult;
        }

        return VulkanRenderCoordinator.CoordinatorResult.success(
                "Vulkan initialized and textures prepared");
    }

    String getLastVulkanError() {
        return nativeLib.getLastVulkanError();
    }

    void resize(int width, int height) {
        if (initialized) {
            nativeLib.resize(width, height);
        }
    }

    void release() {
        if (!initialized) {
            return;
        }
        textureAssetLoader.clearCache();
        nativeLib.cleanupVulkan();
        initialized = false;
    }

    boolean isInitialized() {
        return initialized;
    }

    private VulkanRenderCoordinator.CoordinatorResult prepareSceneTextures() {
        for (TextureSpec textureSpec : sceneDefinition.textureSpecs()) {
            TextureUploadResult uploadResult = textureAssetLoader.uploadTexture(
                    textureSpec.getAssetName(),
                    textureSpec.getAlphaBlend());
            if (!uploadResult.isSuccess()) {
                return VulkanRenderCoordinator.CoordinatorResult.failure(
                        VulkanRenderCoordinator.CoordinatorErrorCode.TEXTURE_UPLOAD_FAILED,
                        "Texture upload failed for " + textureSpec.getAssetName()
                                + " [" + uploadResult.getErrorCode() + "]: "
                                + uploadResult.getMessage());
            }
        }
        for (ShapeTextureRegistration shapeTexture : sceneDefinition.shapeTextureRegistrations()) {
            ShapeTextureUploadResult uploadResult = generatedShapeTextureLoader.uploadShapeTextureToSlot(
                    shapeTexture.getTextureSpec(),
                    shapeTexture.getTextureSlot());
            if (!uploadResult.isSuccess()) {
                return VulkanRenderCoordinator.CoordinatorResult.failure(
                        VulkanRenderCoordinator.CoordinatorErrorCode.TEXTURE_UPLOAD_FAILED,
                        "Shape texture upload failed for " + shapeTexture.getTextureSpec().getDebugName()
                                + " [" + uploadResult.getErrorCode() + "]: "
                                + uploadResult.getMessage());
            }
        }
        return VulkanRenderCoordinator.CoordinatorResult.success(
                "Scene textures prepared");
    }
}
