package com.hsw.vulkansmokehost;

import android.app.Activity;
import android.content.res.AssetManager;
import android.view.Surface;

import com.hsw.vulkanrenderingengine.bridge.NativeHostBridge;

import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * Tiny host-facing renderer facade used by the smoke app.
 */
public final class SmokeSampleRenderer {

    public static final int SMOKE_TEXTURE_SLOT = 0;
    public static final int PING_TEXTURE_SLOT = 1;
    public static final int WIFI_TEXTURE_SLOT = 2;

    private static final int TEXTURE_SLOT = SMOKE_TEXTURE_SLOT;
    private static final int BLEND_ALPHA = 0;
    private static final int SCENE_PAYLOAD_VERSION = 1;
    private static final String SCENE_MODE = "maca8x1 atlas + scissor clip + ping/wifi";
    private static final int ATLAS_FRAME_COUNT = 8;
    private static final float ATLAS_FRAME_WIDTH_UV = 1.0f / ATLAS_FRAME_COUNT;
    private static final float ATLAS_FRAMES_PER_SECOND = 10.0f;
    private static final int REFERENCE_SPRITE = 0;
    private static final int CLIPPED_SPRITE = 1;
    private static final int PING_SPRITE = 2;
    private static final int WIFI_SPRITE = 3;
    private static final int SPRITE_COUNT = 4;

    private final int[] previousTextureSlots = intValues(SPRITE_COUNT, TEXTURE_SLOT);
    private final int[] previousBlendModes = intValues(SPRITE_COUNT, BLEND_ALPHA);
    private final int[] previousLayers = intValues(SPRITE_COUNT, 0);
    private final int[] previousRenderOrders = intValues(SPRITE_COUNT, 0);
    private final int[] previousScissorXs = intValues(SPRITE_COUNT, 0);
    private final int[] previousScissorYs = intValues(SPRITE_COUNT, 0);
    private final int[] previousScissorWidths = intValues(SPRITE_COUNT, 0);
    private final int[] previousScissorHeights = intValues(SPRITE_COUNT, 0);
    private final float[] previousTextureUs = floatValues(SPRITE_COUNT, 0.0f);
    private final float[] previousTextureVs = floatValues(SPRITE_COUNT, 0.0f);
    private final float[] previousTextureWidthUvs = floatValues(SPRITE_COUNT, ATLAS_FRAME_WIDTH_UV);
    private final float[] previousTextureHeightUvs = floatValues(SPRITE_COUNT, 1.0f);
    private final float[] previousXs = floatValues(SPRITE_COUNT, 0.0f);
    private final float[] previousYs = floatValues(SPRITE_COUNT, 0.0f);
    private final float[] previousZs = floatValues(SPRITE_COUNT, 0.0f);
    private final float[] previousScaleXs = floatValues(SPRITE_COUNT, 0.36f);
    private final float[] previousScaleYs = floatValues(SPRITE_COUNT, 0.36f);
    private final float[] previousRotationDegs = floatValues(SPRITE_COUNT, 0.0f);
    private final float[] previousAnimationStates = floatValues(SPRITE_COUNT, 0.0f);

    private final int[] currentTextureSlots = intValues(SPRITE_COUNT, TEXTURE_SLOT);
    private final int[] currentBlendModes = intValues(SPRITE_COUNT, BLEND_ALPHA);
    private final int[] currentLayers = intValues(SPRITE_COUNT, 0);
    private final int[] currentRenderOrders = intValues(SPRITE_COUNT, 0);
    private final int[] currentScissorXs = intValues(SPRITE_COUNT, 0);
    private final int[] currentScissorYs = intValues(SPRITE_COUNT, 0);
    private final int[] currentScissorWidths = intValues(SPRITE_COUNT, 0);
    private final int[] currentScissorHeights = intValues(SPRITE_COUNT, 0);
    private final float[] currentTextureUs = floatValues(SPRITE_COUNT, 0.0f);
    private final float[] currentTextureVs = floatValues(SPRITE_COUNT, 0.0f);
    private final float[] currentTextureWidthUvs = floatValues(SPRITE_COUNT, ATLAS_FRAME_WIDTH_UV);
    private final float[] currentTextureHeightUvs = floatValues(SPRITE_COUNT, 1.0f);
    private final float[] currentXs = floatValues(SPRITE_COUNT, 0.0f);
    private final float[] currentYs = floatValues(SPRITE_COUNT, 0.0f);
    private final float[] currentZs = floatValues(SPRITE_COUNT, 0.0f);
    private final float[] currentScaleXs = floatValues(SPRITE_COUNT, 0.36f);
    private final float[] currentScaleYs = floatValues(SPRITE_COUNT, 0.36f);
    private final float[] currentRotationDegs = floatValues(SPRITE_COUNT, 0.0f);
    private final float[] currentAnimationStates = floatValues(SPRITE_COUNT, 0.0f);
    private final NativeHostBridge rendererBridge;

    private int currentAtlasFrame;

    public SmokeSampleRenderer() {
        rendererBridge = NativeHostBridge.createJni();

        previousTextureSlots[PING_SPRITE] = PING_TEXTURE_SLOT;
        currentTextureSlots[PING_SPRITE] = PING_TEXTURE_SLOT;
        previousTextureSlots[WIFI_SPRITE] = WIFI_TEXTURE_SLOT;
        currentTextureSlots[WIFI_SPRITE] = WIFI_TEXTURE_SLOT;

        previousLayers[CLIPPED_SPRITE] = 1;
        currentLayers[CLIPPED_SPRITE] = 1;
        previousRenderOrders[CLIPPED_SPRITE] = 1;
        currentRenderOrders[CLIPPED_SPRITE] = 1;
        previousLayers[PING_SPRITE] = 2;
        currentLayers[PING_SPRITE] = 2;
        previousRenderOrders[PING_SPRITE] = 2;
        currentRenderOrders[PING_SPRITE] = 2;
        previousLayers[WIFI_SPRITE] = 3;
        currentLayers[WIFI_SPRITE] = 3;
        previousRenderOrders[WIFI_SPRITE] = 3;
        currentRenderOrders[WIFI_SPRITE] = 3;
    }

    public boolean init(Activity activity, Surface surface, AssetManager assetManager) {
        return rendererBridge.initVulkan(activity, surface, assetManager);
    }

    public void resize(int width, int height) {
        rendererBridge.resize(width, height);
    }

    public boolean renderSmokeFrame(float elapsedSeconds, int viewportWidth, int viewportHeight) {
        float safeSeconds = Float.isFinite(elapsedSeconds) ? elapsedSeconds : 0.0f;
        currentAtlasFrame = Math.floorMod((int) (safeSeconds * ATLAS_FRAMES_PER_SECOND), ATLAS_FRAME_COUNT);
        float atlasU = currentAtlasFrame * ATLAS_FRAME_WIDTH_UV;
        float atlasState = currentAtlasFrame / (float) (ATLAS_FRAME_COUNT - 1);

        setSpriteState(REFERENCE_SPRITE,
                -0.62f,
                -0.42f + (float) Math.sin(safeSeconds * 1.2f) * 0.04f,
                0.32f,
                0.32f,
                0.0f,
                atlasU,
                0.0f,
                ATLAS_FRAME_WIDTH_UV,
                1.0f,
                0,
                0,
                0,
                0,
                atlasState);

        int clipWidth = Math.max(1, Math.round(viewportWidth * 0.42f));
        int clipHeight = Math.max(1, Math.round(viewportHeight * 0.42f));
        int clipX = Math.max(0, (viewportWidth - clipWidth) / 2);
        int clipY = Math.max(0, (viewportHeight - clipHeight) / 2);
        float sweepX = (float) Math.sin(safeSeconds * 0.95f) * 0.44f;
        float bobY = (float) Math.cos(safeSeconds * 1.15f) * 0.08f;
        setSpriteState(CLIPPED_SPRITE,
                sweepX,
                bobY,
                0.86f,
                0.86f,
                (float) Math.sin(safeSeconds * 1.7f) * 8.0f,
                atlasU,
                0.0f,
                ATLAS_FRAME_WIDTH_UV,
                1.0f,
                clipX,
                clipY,
                clipWidth,
                clipHeight,
                atlasState);

        setSpriteState(PING_SPRITE,
                -0.58f,
                0.48f,
                0.20f,
                0.20f,
                0.0f,
                0.0f,
                0.0f,
                1.0f,
                1.0f,
                0,
                0,
                0,
                0,
                0.0f);

        setSpriteState(WIFI_SPRITE,
                0.58f,
                0.48f,
                0.22f,
                0.22f,
                safeSeconds * 18.0f,
                0.0f,
                0.0f,
                1.0f,
                1.0f,
                0,
                0,
                0,
                0,
                0.0f);

        boolean submitted = rendererBridge.submitReadOnlySceneFrame(SCENE_PAYLOAD_VERSION,
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
                0.0f,
                0.0f,
                1.0f,
                0.0f,
                0.0f,
                0.0f,
                1.0f,
                0.0f,
                1.0f);
        if (!submitted) {
            return false;
        }
        if (!rendererBridge.renderFrame()) {
            return false;
        }
        String lastError = rendererBridge.getLastVulkanError();
        return lastError == null || lastError.isEmpty();
    }

    public void cleanup() {
        rendererBridge.cleanupVulkan();
    }

    public String getLastVulkanError() {
        return rendererBridge.getLastVulkanError();
    }

    public String getTextureUploadStatus() {
        return rendererBridge.getTextureUploadStatus();
    }

    public String getSwapchainStatus() {
        return rendererBridge.getSwapchainStatus();
    }

    public String getSceneMode() {
        return SCENE_MODE;
    }

    public int getCurrentAtlasFrame() {
        return currentAtlasFrame;
    }

    public String getClipStatus() {
        return currentScissorXs[CLIPPED_SPRITE]
                + ","
                + currentScissorYs[CLIPPED_SPRITE]
                + " "
                + currentScissorWidths[CLIPPED_SPRITE]
                + "x"
                + currentScissorHeights[CLIPPED_SPRITE];
    }

    public int uploadTextureRgbaToSlot(String debugName,
                                       int width,
                                       int height,
                                       ByteBuffer rgbaBuffer,
                                       int slot) {
        return rendererBridge.uploadTextureRgbaToSlot(debugName, width, height, rgbaBuffer, slot);
    }

    private void setSpriteState(int spriteIndex,
                                float x,
                                float y,
                                float scaleX,
                                float scaleY,
                                float angleDeg,
                                float textureU,
                                float textureV,
                                float textureWidthUv,
                                float textureHeightUv,
                                int scissorX,
                                int scissorY,
                                int scissorWidth,
                                int scissorHeight,
                                float animationState) {
        previousScissorXs[spriteIndex] = currentScissorXs[spriteIndex];
        previousScissorYs[spriteIndex] = currentScissorYs[spriteIndex];
        previousScissorWidths[spriteIndex] = currentScissorWidths[spriteIndex];
        previousScissorHeights[spriteIndex] = currentScissorHeights[spriteIndex];
        previousTextureUs[spriteIndex] = currentTextureUs[spriteIndex];
        previousTextureVs[spriteIndex] = currentTextureVs[spriteIndex];
        previousTextureWidthUvs[spriteIndex] = currentTextureWidthUvs[spriteIndex];
        previousTextureHeightUvs[spriteIndex] = currentTextureHeightUvs[spriteIndex];
        previousXs[spriteIndex] = currentXs[spriteIndex];
        previousYs[spriteIndex] = currentYs[spriteIndex];
        previousScaleXs[spriteIndex] = currentScaleXs[spriteIndex];
        previousScaleYs[spriteIndex] = currentScaleYs[spriteIndex];
        previousRotationDegs[spriteIndex] = currentRotationDegs[spriteIndex];
        previousAnimationStates[spriteIndex] = currentAnimationStates[spriteIndex];

        currentScissorXs[spriteIndex] = scissorX;
        currentScissorYs[spriteIndex] = scissorY;
        currentScissorWidths[spriteIndex] = scissorWidth;
        currentScissorHeights[spriteIndex] = scissorHeight;
        currentTextureUs[spriteIndex] = textureU;
        currentTextureVs[spriteIndex] = textureV;
        currentTextureWidthUvs[spriteIndex] = textureWidthUv;
        currentTextureHeightUvs[spriteIndex] = textureHeightUv;
        currentXs[spriteIndex] = x;
        currentYs[spriteIndex] = y;
        currentScaleXs[spriteIndex] = scaleX;
        currentScaleYs[spriteIndex] = scaleY;
        currentRotationDegs[spriteIndex] = angleDeg;
        currentAnimationStates[spriteIndex] = animationState;
    }

    private static int[] intValues(int size, int value) {
        int[] values = new int[size];
        Arrays.fill(values, value);
        return values;
    }

    private static float[] floatValues(int size, float value) {
        float[] values = new float[size];
        Arrays.fill(values, value);
        return values;
    }

}
