package com.hsw.etalonapp.engine.api;

import android.app.Activity;
import android.content.res.AssetManager;
import android.view.Surface;

import com.hsw.vulkanrenderingengine.bridge.NativeHostBridge;

import java.nio.ByteBuffer;
import java.util.Objects;

/**
 * Engine-facing native renderer facade.
 *
 * Java engine code owns loop, scene, asset, and audio orchestration. This class
 * is the narrow Vulkan/JNI backend facade: render state crosses as a complete
 * immutable SceneFrame, and native code may copy it into renderer-owned buffers.
 */
public class NativeLib {

    private final NativeHostBridge bridge;

    public NativeLib() {
        this(NativeHostBridge.createJni());
    }

    NativeLib(NativeHostBridge bridge) {
        this.bridge = Objects.requireNonNull(bridge, "bridge");
    }

    public boolean initVulkan(Activity activity, Surface surface, AssetManager assetManager) {
        return bridge.initVulkan(activity, surface, assetManager);
    }

    public void resize(int width, int height) {
        bridge.resize(width, height);
    }

    public void cleanupVulkan() {
        bridge.cleanupVulkan();
    }

    public String getLastVulkanError() {
        return bridge.getLastVulkanError();
    }

    /**
     * Uploads straight RGBA pixels. Native rendering converts them to premultiplied
     * alpha once before atlas packing and keeps Vulkan blending in premultiplied mode.
     */
    public int uploadTextureRgba(String debugName, int width, int height, ByteBuffer rgbaBuffer) {
        return bridge.uploadTextureRgba(debugName, width, height, rgbaBuffer);
    }

    /**
     * Uploads straight RGBA pixels into a fixed logical slot. Native rendering converts
     * them to premultiplied alpha once before atlas packing.
     */
    public int uploadTextureRgbaToSlot(String debugName, int width, int height, ByteBuffer rgbaBuffer, int slot) {
        return bridge.uploadTextureRgbaToSlot(debugName, width, height, rgbaBuffer, slot);
    }

    public boolean releaseTexture(int textureSlot) {
        return bridge.releaseTexture(textureSlot);
    }

    public void clearTextureCache() {
        bridge.clearTextureCache();
    }

    public boolean renderSceneFrame(SceneFrame sceneFrame) {
        if (!submitReadOnlySceneFrame(sceneFrame)) {
            return false;
        }
        return bridge.renderFrame();
    }

    private boolean submitReadOnlySceneFrame(SceneFrame sceneFrame) {
        SceneFrame nonNullSceneFrame = Objects.requireNonNull(sceneFrame, "sceneFrame");
        SceneSnapshot previousSnapshot = nonNullSceneFrame.previousSceneSnapshot();
        SceneSnapshot currentSnapshot = nonNullSceneFrame.currentSceneSnapshot();
        OrthoCamera previousCamera = nonNullSceneFrame.previousCamera();
        OrthoCamera currentCamera = nonNullSceneFrame.currentCamera();

        return bridge.submitReadOnlySceneFrame(SceneSubmissionContract.PAYLOAD_VERSION,
                previousSnapshot.textureSlotsRaw(),
                previousSnapshot.blendModesRaw(),
                previousSnapshot.layersRaw(),
                previousSnapshot.renderOrdersRaw(),
                previousSnapshot.scissorXsRaw(),
                previousSnapshot.scissorYsRaw(),
                previousSnapshot.scissorWidthsRaw(),
                previousSnapshot.scissorHeightsRaw(),
                previousSnapshot.textureUsRaw(),
                previousSnapshot.textureVsRaw(),
                previousSnapshot.textureWidthUvsRaw(),
                previousSnapshot.textureHeightUvsRaw(),
                previousSnapshot.xsRaw(),
                previousSnapshot.ysRaw(),
                previousSnapshot.zsRaw(),
                previousSnapshot.scaleXsRaw(),
                previousSnapshot.scaleYsRaw(),
                previousSnapshot.rotationDegsRaw(),
                previousSnapshot.animationStatesRaw(),
                currentSnapshot.textureSlotsRaw(),
                currentSnapshot.blendModesRaw(),
                currentSnapshot.layersRaw(),
                currentSnapshot.renderOrdersRaw(),
                currentSnapshot.scissorXsRaw(),
                currentSnapshot.scissorYsRaw(),
                currentSnapshot.scissorWidthsRaw(),
                currentSnapshot.scissorHeightsRaw(),
                currentSnapshot.textureUsRaw(),
                currentSnapshot.textureVsRaw(),
                currentSnapshot.textureWidthUvsRaw(),
                currentSnapshot.textureHeightUvsRaw(),
                currentSnapshot.xsRaw(),
                currentSnapshot.ysRaw(),
                currentSnapshot.zsRaw(),
                currentSnapshot.scaleXsRaw(),
                currentSnapshot.scaleYsRaw(),
                currentSnapshot.rotationDegsRaw(),
                currentSnapshot.animationStatesRaw(),
                previousCamera.getX(),
                previousCamera.getY(),
                previousCamera.getZoom(),
                previousCamera.getRotationDeg(),
                currentCamera.getX(),
                currentCamera.getY(),
                currentCamera.getZoom(),
                currentCamera.getRotationDeg(),
                nonNullSceneFrame.getInterpolationAlpha());
    }
}
