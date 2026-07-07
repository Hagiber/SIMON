package com.hsw.etalonapp.engine.api;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.app.Activity;
import android.content.res.AssetManager;
import android.view.Surface;

import com.hsw.vulkanrenderingengine.bridge.NativeHostBridge;

import org.junit.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class NativeLibBoundaryTest {

    @Test
    public void publicApi_exposesOnlyFrameBasedRenderEntryPoint() throws Exception {
        Set<String> allowedPublicMethods = new HashSet<>(Arrays.asList(
                "initVulkan",
                "resize",
                "cleanupVulkan",
                "getLastVulkanError",
                "uploadTextureRgba",
                "uploadTextureRgbaToSlot",
                "releaseTexture",
                "clearTextureCache",
                "renderSceneFrame"
        ));

        for (Method method : NativeLib.class.getMethods()) {
            if (method.getDeclaringClass() != NativeLib.class ||
                    !Modifier.isPublic(method.getModifiers())) {
                continue;
            }
            assertTrue("Unexpected public NativeLib method: " + method.getName(),
                    allowedPublicMethods.contains(method.getName()));
        }
        assertEquals(boolean.class, NativeLib.class
                .getMethod("renderSceneFrame", SceneFrame.class)
                .getReturnType());
        assertFalse(hasPublicNativeLibMethod("renderFrame"));
        assertFalse(hasPublicNativeLibMethod("submitSceneFrame"));
    }

    @Test
    public void renderSceneFrame_submitsImmutableSnapshotThenRenders() {
        RecordingBridge bridge = new RecordingBridge();
        NativeLib nativeLib = new NativeLib(bridge);

        int[] previousTextureSlots = new int[]{5};
        float[] previousXs = new float[]{1.0f};
        SceneSnapshot previousSnapshot = SceneSnapshot.of(previousTextureSlots,
                previousXs,
                new float[]{2.0f},
                new float[]{3.0f},
                new float[]{4.0f},
                new float[]{5.0f});

        previousTextureSlots[0] = 99;
        previousXs[0] = 99.0f;

        SceneSnapshot currentSnapshot = SceneSnapshot.of(new int[]{5},
                new float[]{6.0f},
                new float[]{7.0f},
                new float[]{8.0f},
                new float[]{9.0f},
                new float[]{10.0f});
        SceneFrame sceneFrame = SceneFrame.of(previousSnapshot, currentSnapshot, 0.25f);

        assertTrue(nativeLib.renderSceneFrame(sceneFrame));

        assertEquals(1, bridge.submitCount);
        assertEquals(1, bridge.renderCount);
        assertEquals(SceneSubmissionContract.PAYLOAD_VERSION, bridge.payloadVersion);
        assertArrayEquals(new int[]{5}, bridge.previousTextureSlots);
        assertArrayEquals(new float[]{1.0f}, bridge.previousXs, 0.0001f);
        assertArrayEquals(new float[]{6.0f}, bridge.currentXs, 0.0001f);
        assertEquals(0.25f, bridge.interpolationAlpha, 0.0001f);
    }

    @Test
    public void renderSceneFrame_doesNotRenderWhenReadOnlySnapshotSubmitFails() {
        RecordingBridge bridge = new RecordingBridge();
        bridge.submitResult = false;
        NativeLib nativeLib = new NativeLib(bridge);

        assertFalse(nativeLib.renderSceneFrame(SceneFrame.empty(0.0f)));

        assertEquals(1, bridge.submitCount);
        assertEquals(0, bridge.renderCount);
    }

    @Test
    public void renderSceneFrame_reportsRenderFrameFailure() {
        RecordingBridge bridge = new RecordingBridge();
        bridge.renderResult = false;
        NativeLib nativeLib = new NativeLib(bridge);

        assertFalse(nativeLib.renderSceneFrame(SceneFrame.empty(0.0f)));

        assertEquals(1, bridge.submitCount);
        assertEquals(1, bridge.renderCount);
    }

    private static boolean hasPublicNativeLibMethod(String name) {
        for (Method method : NativeLib.class.getMethods()) {
            if (method.getDeclaringClass() == NativeLib.class &&
                    Modifier.isPublic(method.getModifiers()) &&
                    method.getName().equals(name)) {
                return true;
            }
        }
        return false;
    }

    private static final class RecordingBridge implements NativeHostBridge {
        private boolean submitResult = true;
        private boolean renderResult = true;
        private int submitCount;
        private int renderCount;
        private int payloadVersion;
        private int[] previousTextureSlots;
        private float[] previousXs;
        private float[] currentXs;
        private float interpolationAlpha;

        @Override
        public boolean initVulkan(Activity activity, Surface surface, AssetManager assetManager) {
            return true;
        }

        @Override
        public boolean renderFrame() {
            renderCount++;
            return renderResult;
        }

        @Override
        public void resize(int width, int height) {
        }

        @Override
        public void cleanupVulkan() {
        }

        @Override
        public String getLastVulkanError() {
            return "";
        }

        @Override
        public String getTextureUploadStatus() {
            return "";
        }

        @Override
        public String getSwapchainStatus() {
            return "";
        }

        @Override
        public int uploadTextureRgba(String debugName, int width, int height, ByteBuffer rgbaBuffer) {
            return 1;
        }

        @Override
        public int uploadTextureRgbaToSlot(String debugName, int width, int height, ByteBuffer rgbaBuffer, int slot) {
            return slot + 1;
        }

        @Override
        public boolean releaseTexture(int textureSlot) {
            return true;
        }

        @Override
        public void clearTextureCache() {
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
            submitCount++;
            this.payloadVersion = payloadVersion;
            this.previousTextureSlots = Arrays.copyOf(previousTextureSlots, previousTextureSlots.length);
            this.previousXs = Arrays.copyOf(previousXs, previousXs.length);
            this.currentXs = Arrays.copyOf(currentXs, currentXs.length);
            this.interpolationAlpha = interpolationAlpha;
            return submitResult;
        }
    }
}
