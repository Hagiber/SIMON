package com.hsw.vulkanrenderingengine.bridge;

import android.app.Activity;
import android.content.res.AssetManager;
import android.view.Surface;

import java.nio.ByteBuffer;

public interface NativeHostBridge {

    static NativeHostBridge createJni() {
        return new JniNativeHostBridge();
    }

    boolean initVulkan(Activity activity, Surface surface, AssetManager assetManager);

    boolean renderFrame();

    void resize(int width, int height);

    void cleanupVulkan();

    String getLastVulkanError();

    String getTextureUploadStatus();

    String getSwapchainStatus();

    int uploadTextureRgba(String debugName, int width, int height, ByteBuffer rgbaBuffer);

    int uploadTextureRgbaToSlot(String debugName, int width, int height, ByteBuffer rgbaBuffer, int slot);

    boolean releaseTexture(int textureSlot);

    void clearTextureCache();

    boolean submitReadOnlySceneFrame(int payloadVersion,
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
                                     float interpolationAlpha);
}
