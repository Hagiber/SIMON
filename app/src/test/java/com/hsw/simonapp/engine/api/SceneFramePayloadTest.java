package com.hsw.simonapp.engine.api;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

public class SceneFramePayloadTest {

    @Test
    public void snapshotOf_usesUnitScaleForLegacyPayloads() {
        SceneSnapshot snapshot = SceneSnapshot.of(new int[]{2},
                new float[]{0.1f},
                new float[]{0.2f},
                new float[]{0.3f},
                new float[]{15.0f},
                new float[]{0.4f});

        assertArrayEquals(new float[]{1.0f}, snapshot.scaleXsRaw(), 0.0001f);
        assertArrayEquals(new float[]{1.0f}, snapshot.scaleYsRaw(), 0.0001f);
        assertArrayEquals(new int[]{BlendMode.ALPHA.nativeValue()}, snapshot.blendModesRaw());
        assertArrayEquals(new int[]{0}, snapshot.layersRaw());
        assertArrayEquals(new int[]{0}, snapshot.renderOrdersRaw());
        assertArrayEquals(new int[]{0}, snapshot.scissorWidthsRaw());
        assertArrayEquals(new int[]{0}, snapshot.scissorHeightsRaw());
    }

    @Test
    public void snapshotOf_acceptsExplicitTransformScale() {
        SceneSnapshot snapshot = SceneSnapshot.of(new int[]{2},
                new float[]{0.1f},
                new float[]{0.2f},
                new float[]{0.3f},
                new float[]{1.5f},
                new float[]{0.75f},
                new float[]{15.0f},
                new float[]{0.4f});

        assertArrayEquals(new float[]{1.5f}, snapshot.scaleXsRaw(), 0.0001f);
        assertArrayEquals(new float[]{0.75f}, snapshot.scaleYsRaw(), 0.0001f);
        assertArrayEquals(new int[]{BlendMode.ALPHA.nativeValue()}, snapshot.blendModesRaw());
    }

    @Test
    public void snapshotOf_acceptsExplicitBlendModes() {
        SceneSnapshot snapshot = SceneSnapshot.of(new int[]{2, 3},
                new float[]{0.1f, 0.2f},
                new float[]{0.2f, 0.3f},
                new float[]{0.3f, 0.4f},
                new float[]{15.0f, 30.0f},
                new float[]{0.4f, 0.5f},
                new BlendMode[]{BlendMode.ADDITIVE, BlendMode.MULTIPLY});

        assertArrayEquals(new int[]{
                BlendMode.ADDITIVE.nativeValue(),
                BlendMode.MULTIPLY.nativeValue()
        }, snapshot.blendModesRaw());
    }

    @Test
    public void snapshotOf_acceptsLayerOrderAndScissorPayload() {
        SceneSnapshot snapshot = SceneSnapshot.of(new int[]{2, 3},
                new float[]{0.1f, 0.2f},
                new float[]{0.2f, 0.3f},
                new float[]{0.3f, 0.4f},
                new float[]{15.0f, 30.0f},
                new float[]{0.4f, 0.5f},
                new BlendMode[]{BlendMode.ADDITIVE, BlendMode.MULTIPLY},
                new int[]{-1, 4},
                new int[]{20, 10},
                new ScissorRect[]{ScissorRect.disabled(), ScissorRect.of(8, 12, 64, 32)});

        assertArrayEquals(new int[]{-1, 4}, snapshot.layersRaw());
        assertArrayEquals(new int[]{20, 10}, snapshot.renderOrdersRaw());
        assertArrayEquals(new int[]{0, 8}, snapshot.scissorXsRaw());
        assertArrayEquals(new int[]{0, 12}, snapshot.scissorYsRaw());
        assertArrayEquals(new int[]{0, 64}, snapshot.scissorWidthsRaw());
        assertArrayEquals(new int[]{0, 32}, snapshot.scissorHeightsRaw());
    }

    @Test(expected = IllegalArgumentException.class)
    public void snapshotOf_rejectsMismatchedLayerPayload() {
        SceneSnapshot.of(new int[]{2},
                new float[]{0.1f},
                new float[]{0.2f},
                new float[]{0.3f},
                new float[]{15.0f},
                new float[]{0.4f},
                new BlendMode[]{BlendMode.ALPHA},
                new int[]{0, 1},
                new int[]{0},
                new ScissorRect[]{ScissorRect.disabled()});
    }

    @Test
    public void snapshotOf_rejectsMismatchedArrayWithFieldName() {
        IllegalArgumentException exception = expectIllegalArgument(() -> SceneSnapshot.of(new int[]{2},
                new float[]{0.1f, 0.2f},
                new float[]{0.2f},
                new float[]{0.3f},
                new float[]{15.0f},
                new float[]{0.4f}));

        assertTrue(exception.getMessage().contains("xs length 2"));
        assertTrue(exception.getMessage().contains("textureSlots length 1"));
    }

    @Test
    public void snapshotOf_rejectsSpriteCountAboveContractLimit() {
        int size = SceneSubmissionContract.MAX_SPRITES + 1;
        int[] textureSlots = new int[size];
        float[] values = new float[size];

        IllegalArgumentException exception = expectIllegalArgument(() -> SceneSnapshot.of(textureSlots,
                values,
                values,
                values,
                values,
                values));

        assertTrue(exception.getMessage().contains("sprite count " + size));
        assertTrue(exception.getMessage().contains("max " + SceneSubmissionContract.MAX_SPRITES));
    }

    @Test
    public void snapshotOf_rejectsTextureSlotsOutsideContractRange() {
        IllegalArgumentException negativeSlot = expectIllegalArgument(() -> SceneSnapshot.of(new int[]{-1},
                new float[]{0.1f},
                new float[]{0.2f},
                new float[]{0.3f},
                new float[]{15.0f},
                new float[]{0.4f}));

        assertTrue(negativeSlot.getMessage().contains("textureSlots[0]"));
        assertTrue(negativeSlot.getMessage().contains("between 0 and " + SceneSubmissionContract.MAX_TEXTURE_SLOT));

        IllegalArgumentException highSlot = expectIllegalArgument(() -> SceneSnapshot.of(
                new int[]{SceneSubmissionContract.MAX_TEXTURE_SLOT + 1},
                new float[]{0.1f},
                new float[]{0.2f},
                new float[]{0.3f},
                new float[]{15.0f},
                new float[]{0.4f}));

        assertTrue(highSlot.getMessage().contains("textureSlots[0]"));
        assertTrue(highSlot.getMessage().contains("but was " + (SceneSubmissionContract.MAX_TEXTURE_SLOT + 1)));
    }

    @Test
    public void snapshotOf_rejectsBlendModePayloadProblemsWithFieldName() {
        IllegalArgumentException wrongLength = expectIllegalArgument(() -> SceneSnapshot.of(new int[]{2},
                new float[]{0.1f},
                new float[]{0.2f},
                new float[]{0.3f},
                new float[]{15.0f},
                new float[]{0.4f},
                new BlendMode[]{BlendMode.ALPHA, BlendMode.ADDITIVE}));

        assertTrue(wrongLength.getMessage().contains("blendModes length 2"));
        assertTrue(wrongLength.getMessage().contains("textureSlots length 1"));

        NullPointerException nullBlendMode = expectNullPointer(() -> SceneSnapshot.of(new int[]{2},
                new float[]{0.1f},
                new float[]{0.2f},
                new float[]{0.3f},
                new float[]{15.0f},
                new float[]{0.4f},
                new BlendMode[]{null}));

        assertTrue(nullBlendMode.getMessage().contains("blendModes[0]"));
    }

    @Test
    public void frameOf_keepsCameraPayload() {
        SceneSnapshot snapshot = SceneSnapshot.empty();
        OrthoCamera previousCamera = OrthoCamera.of(-1.0f, 0.5f, 1.25f, 10.0f);
        OrthoCamera currentCamera = OrthoCamera.of(1.0f, -0.5f, 2.0f, 20.0f);

        SceneFrame frame = SceneFrame.of(snapshot, snapshot, previousCamera, currentCamera, 0.5f);

        assertSame(previousCamera, frame.previousCamera());
        assertSame(currentCamera, frame.currentCamera());
        assertEquals(0.5f, frame.getInterpolationAlpha(), 0.0001f);
    }

    @Test(expected = IllegalArgumentException.class)
    public void cameraRejectsNonPositiveZoom() {
        OrthoCamera.of(0.0f, 0.0f, 0.0f, 0.0f);
    }

    private static IllegalArgumentException expectIllegalArgument(Runnable action) {
        try {
            action.run();
            fail("Expected IllegalArgumentException");
            return null;
        } catch (IllegalArgumentException exception) {
            return exception;
        }
    }

    private static NullPointerException expectNullPointer(Runnable action) {
        try {
            action.run();
            fail("Expected NullPointerException");
            return null;
        } catch (NullPointerException exception) {
            return exception;
        }
    }
}
