package com.hsw.vulkanrenderingengine.bridge;

import android.app.Activity;
import android.content.res.AssetManager;
import android.view.Surface;

import java.nio.ByteBuffer;

final class JniNativeHostBridge implements NativeHostBridge {

    private final NativeLibBridge bridge = new NativeLibBridge();

    @Override
    public boolean initVulkan(Activity activity, Surface surface, AssetManager assetManager) {
        return bridge.initVulkan(activity, surface, assetManager);
    }

    @Override
    public boolean renderFrame() {
        return bridge.renderFrame();
    }

    @Override
    public void resize(int width, int height) {
        bridge.resize(width, height);
    }

    @Override
    public void cleanupVulkan() {
        bridge.cleanupVulkan();
    }

    @Override
    public String getLastVulkanError() {
        return bridge.getLastVulkanError();
    }

    @Override
    public String getTextureUploadStatus() {
        return bridge.getTextureUploadStatus();
    }

    @Override
    public String getSwapchainStatus() {
        return bridge.getSwapchainStatus();
    }

    @Override
    public int uploadTextureRgba(String debugName, int width, int height, ByteBuffer rgbaBuffer) {
        return bridge.uploadTextureRgba(debugName, width, height, rgbaBuffer);
    }

    @Override
    public int uploadTextureRgbaToSlot(String debugName, int width, int height, ByteBuffer rgbaBuffer, int slot) {
        return bridge.uploadTextureRgbaToSlot(debugName, width, height, rgbaBuffer, slot);
    }

    @Override
    public boolean releaseTexture(int textureSlot) {
        return bridge.releaseTexture(textureSlot);
    }

    @Override
    public void clearTextureCache() {
        bridge.clearTextureCache();
    }

    @Override
    public boolean submitReadOnlySceneFrame(int payloadVersion,
                                            int[] previousTextureSlots,
                                            int[] previousBlendModes,
                                            int[] previousLayers,
                                            int[] previousRenderOrders,
                                            int[] previousScissorXs,
                                            int[] previousScissorYs,
                                            int[] previousScissorWidths,
                                            int[] previousScissorHeights,
                                            float[] previousTextureUs,
                                            float[] previousTextureVs,
                                            float[] previousTextureWidthUvs,
                                            float[] previousTextureHeightUvs,
                                            float[] previousXs,
                                            float[] previousYs,
                                            float[] previousZs,
                                            float[] previousScaleXs,
                                            float[] previousScaleYs,
                                            float[] previousRotationDegs,
                                            float[] previousAnimationStates,
                                            int[] currentTextureSlots,
                                            int[] currentBlendModes,
                                            int[] currentLayers,
                                            int[] currentRenderOrders,
                                            int[] currentScissorXs,
                                            int[] currentScissorYs,
                                            int[] currentScissorWidths,
                                            int[] currentScissorHeights,
                                            float[] currentTextureUs,
                                            float[] currentTextureVs,
                                            float[] currentTextureWidthUvs,
                                            float[] currentTextureHeightUvs,
                                            float[] currentXs,
                                            float[] currentYs,
                                            float[] currentZs,
                                            float[] currentScaleXs,
                                            float[] currentScaleYs,
                                            float[] currentRotationDegs,
                                            float[] currentAnimationStates,
                                            float previousCameraX,
                                            float previousCameraY,
                                            float previousCameraZoom,
                                            float previousCameraRotationDeg,
                                            float currentCameraX,
                                            float currentCameraY,
                                            float currentCameraZoom,
                                            float currentCameraRotationDeg,
                                            float interpolationAlpha) {
        return bridge.submitReadOnlySceneFrameNative(payloadVersion,
                previousTextureSlots,
                previousBlendModes,
                previousLayers,
                previousRenderOrders,
                previousScissorXs,
                previousScissorYs,
                previousScissorWidths,
                previousScissorHeights,
                previousTextureUs,
                previousTextureVs,
                previousTextureWidthUvs,
                previousTextureHeightUvs,
                previousXs,
                previousYs,
                previousZs,
                previousScaleXs,
                previousScaleYs,
                previousRotationDegs,
                previousAnimationStates,
                currentTextureSlots,
                currentBlendModes,
                currentLayers,
                currentRenderOrders,
                currentScissorXs,
                currentScissorYs,
                currentScissorWidths,
                currentScissorHeights,
                currentTextureUs,
                currentTextureVs,
                currentTextureWidthUvs,
                currentTextureHeightUvs,
                currentXs,
                currentYs,
                currentZs,
                currentScaleXs,
                currentScaleYs,
                currentRotationDegs,
                currentAnimationStates,
                previousCameraX,
                previousCameraY,
                previousCameraZoom,
                previousCameraRotationDeg,
                currentCameraX,
                currentCameraY,
                currentCameraZoom,
                currentCameraRotationDeg,
                interpolationAlpha);
    }
}
