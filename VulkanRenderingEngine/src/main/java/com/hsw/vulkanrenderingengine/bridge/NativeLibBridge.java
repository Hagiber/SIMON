package com.hsw.vulkanrenderingengine.bridge;

import android.app.Activity;
import android.content.res.AssetManager;
import android.view.Surface;

import java.nio.ByteBuffer;

/**
 * Low-level JNI bridge for the VulkanRenderingEngine AAR.
 */
final class NativeLibBridge {

    static {
        System.loadLibrary("vulkanrenderingengine");
    }

    native boolean initVulkan(Activity activity, Surface surface, AssetManager assetManager);

    native boolean renderFrame();

    native void resize(int width, int height);

    native void cleanupVulkan();

    native String getLastVulkanError();

    native String getTextureUploadStatus();

    native String getSwapchainStatus();

    /**
     * The buffer is straight RGBA; native code normalizes it to premultiplied alpha.
     */
    native int uploadTextureRgba(String debugName, int width, int height, ByteBuffer rgbaBuffer);

    /**
     * The buffer is straight RGBA; native code normalizes it to premultiplied alpha.
     */
    native int uploadTextureRgbaToSlot(String debugName, int width, int height, ByteBuffer rgbaBuffer, int slot);

    native boolean releaseTexture(int textureSlot);

    native void clearTextureCache();

    native boolean submitReadOnlySceneFrameNative(int payloadVersion,
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
