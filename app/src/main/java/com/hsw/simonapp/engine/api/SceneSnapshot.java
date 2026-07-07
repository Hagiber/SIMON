package com.hsw.simonapp.engine.api;

import java.util.Arrays;
import java.util.Objects;

public final class SceneSnapshot {

    private static final SceneSnapshot EMPTY = new SceneSnapshot(new int[0],
            new int[0],
            new int[0],
            new int[0],
            new int[0],
            new int[0],
            new int[0],
            new int[0],
            new float[0],
            new float[0],
            new float[0],
            new float[0],
            new float[0],
            new float[0],
            new float[0],
            new float[0],
            new float[0],
            new float[0],
            new float[0],
            false);

    private final int[] textureSlots;
    private final int[] blendModes;
    private final int[] layers;
    private final int[] renderOrders;
    private final int[] scissorXs;
    private final int[] scissorYs;
    private final int[] scissorWidths;
    private final int[] scissorHeights;
    private final float[] textureUs;
    private final float[] textureVs;
    private final float[] textureWidthUvs;
    private final float[] textureHeightUvs;
    private final float[] xs;
    private final float[] ys;
    private final float[] zs;
    private final float[] scaleXs;
    private final float[] scaleYs;
    private final float[] rotationDegs;
    private final float[] animationStates;

    private SceneSnapshot(int[] textureSlots,
                          int[] blendModes,
                          int[] layers,
                          int[] renderOrders,
                          int[] scissorXs,
                          int[] scissorYs,
                          int[] scissorWidths,
                          int[] scissorHeights,
                          float[] textureUs,
                          float[] textureVs,
                          float[] textureWidthUvs,
                          float[] textureHeightUvs,
                          float[] xs,
                          float[] ys,
                          float[] zs,
                          float[] scaleXs,
                          float[] scaleYs,
                          float[] rotationDegs,
                          float[] animationStates,
                          boolean copyArrays) {
        this.textureSlots = copyArrays ? Arrays.copyOf(textureSlots, textureSlots.length) : textureSlots;
        this.blendModes = copyArrays ? Arrays.copyOf(blendModes, blendModes.length) : blendModes;
        this.layers = copyArrays ? Arrays.copyOf(layers, layers.length) : layers;
        this.renderOrders = copyArrays ? Arrays.copyOf(renderOrders, renderOrders.length) : renderOrders;
        this.scissorXs = copyArrays ? Arrays.copyOf(scissorXs, scissorXs.length) : scissorXs;
        this.scissorYs = copyArrays ? Arrays.copyOf(scissorYs, scissorYs.length) : scissorYs;
        this.scissorWidths = copyArrays ? Arrays.copyOf(scissorWidths, scissorWidths.length) : scissorWidths;
        this.scissorHeights = copyArrays ? Arrays.copyOf(scissorHeights, scissorHeights.length) : scissorHeights;
        this.textureUs = copyArrays ? Arrays.copyOf(textureUs, textureUs.length) : textureUs;
        this.textureVs = copyArrays ? Arrays.copyOf(textureVs, textureVs.length) : textureVs;
        this.textureWidthUvs = copyArrays ? Arrays.copyOf(textureWidthUvs, textureWidthUvs.length) : textureWidthUvs;
        this.textureHeightUvs = copyArrays ? Arrays.copyOf(textureHeightUvs, textureHeightUvs.length) : textureHeightUvs;
        this.xs = copyArrays ? Arrays.copyOf(xs, xs.length) : xs;
        this.ys = copyArrays ? Arrays.copyOf(ys, ys.length) : ys;
        this.zs = copyArrays ? Arrays.copyOf(zs, zs.length) : zs;
        this.scaleXs = copyArrays ? Arrays.copyOf(scaleXs, scaleXs.length) : scaleXs;
        this.scaleYs = copyArrays ? Arrays.copyOf(scaleYs, scaleYs.length) : scaleYs;
        this.rotationDegs = copyArrays ? Arrays.copyOf(rotationDegs, rotationDegs.length) : rotationDegs;
        this.animationStates = copyArrays ? Arrays.copyOf(animationStates, animationStates.length) : animationStates;
    }

    public static SceneSnapshot empty() {
        return EMPTY;
    }

    public static SceneSnapshot of(int[] textureSlots,
                                   float[] xs,
                                   float[] ys,
                                   float[] zs,
                                   float[] rotationDegs,
                                   float[] animationStates) {
        requirePayloadField(textureSlots, "textureSlots");
        float[] scaleXs = new float[textureSlots.length];
        float[] scaleYs = new float[textureSlots.length];
        Arrays.fill(scaleXs, 1.0f);
        Arrays.fill(scaleYs, 1.0f);
        return of(textureSlots, xs, ys, zs, scaleXs, scaleYs, rotationDegs, animationStates);
    }

    public static SceneSnapshot of(int[] textureSlots,
                                   float[] xs,
                                   float[] ys,
                                   float[] zs,
                                   float[] rotationDegs,
                                   float[] animationStates,
                                   BlendMode[] blendModes) {
        requirePayloadField(textureSlots, "textureSlots");
        float[] scaleXs = new float[textureSlots.length];
        float[] scaleYs = new float[textureSlots.length];
        Arrays.fill(scaleXs, 1.0f);
        Arrays.fill(scaleYs, 1.0f);
        return of(textureSlots, xs, ys, zs, scaleXs, scaleYs, rotationDegs, animationStates, blendModes);
    }

    public static SceneSnapshot of(int[] textureSlots,
                                   float[] xs,
                                   float[] ys,
                                   float[] zs,
                                   float[] rotationDegs,
                                   float[] animationStates,
                                   BlendMode[] blendModes,
                                   int[] layers,
                                   int[] renderOrders) {
        requirePayloadField(textureSlots, "textureSlots");
        float[] scaleXs = new float[textureSlots.length];
        float[] scaleYs = new float[textureSlots.length];
        Arrays.fill(scaleXs, 1.0f);
        Arrays.fill(scaleYs, 1.0f);
        return of(textureSlots,
                xs,
                ys,
                zs,
                scaleXs,
                scaleYs,
                rotationDegs,
                animationStates,
                blendModes,
                layers,
                renderOrders);
    }

    public static SceneSnapshot of(int[] textureSlots,
                                   float[] xs,
                                   float[] ys,
                                   float[] zs,
                                   float[] rotationDegs,
                                   float[] animationStates,
                                   BlendMode[] blendModes,
                                   int[] layers,
                                   int[] renderOrders,
                                   ScissorRect[] scissors) {
        requirePayloadField(textureSlots, "textureSlots");
        float[] scaleXs = new float[textureSlots.length];
        float[] scaleYs = new float[textureSlots.length];
        Arrays.fill(scaleXs, 1.0f);
        Arrays.fill(scaleYs, 1.0f);
        return of(textureSlots,
                xs,
                ys,
                zs,
                scaleXs,
                scaleYs,
                rotationDegs,
                animationStates,
                blendModes,
                layers,
                renderOrders,
                scissors);
    }

    public static SceneSnapshot of(int[] textureSlots,
                                   float[] xs,
                                   float[] ys,
                                   float[] zs,
                                   float[] rotationDegs,
                                   float[] animationStates,
                                   BlendMode[] blendModes,
                                   int[] layers,
                                   int[] renderOrders,
                                   ScissorRect[] scissors,
                                   TextureRegion[] textureRegions) {
        requirePayloadField(textureSlots, "textureSlots");
        float[] scaleXs = new float[textureSlots.length];
        float[] scaleYs = new float[textureSlots.length];
        Arrays.fill(scaleXs, 1.0f);
        Arrays.fill(scaleYs, 1.0f);
        return of(textureSlots,
                xs,
                ys,
                zs,
                scaleXs,
                scaleYs,
                rotationDegs,
                animationStates,
                blendModes,
                layers,
                renderOrders,
                scissors,
                textureRegions);
    }

    public static SceneSnapshot of(int[] textureSlots,
                                   float[] xs,
                                   float[] ys,
                                   float[] zs,
                                   float[] scaleXs,
                                   float[] scaleYs,
                                   float[] rotationDegs,
                                   float[] animationStates) {
        requirePayloadField(textureSlots, "textureSlots");
        return ofNativePayload(textureSlots,
                defaultBlendModes(textureSlots.length),
                defaultIntValues(textureSlots.length),
                defaultIntValues(textureSlots.length),
                defaultIntValues(textureSlots.length),
                defaultIntValues(textureSlots.length),
                defaultIntValues(textureSlots.length),
                defaultIntValues(textureSlots.length),
                defaultFloatValues(textureSlots.length, 0.0f),
                defaultFloatValues(textureSlots.length, 0.0f),
                defaultFloatValues(textureSlots.length, 1.0f),
                defaultFloatValues(textureSlots.length, 1.0f),
                xs,
                ys,
                zs,
                scaleXs,
                scaleYs,
                rotationDegs,
                animationStates);
    }

    public static SceneSnapshot of(int[] textureSlots,
                                   float[] xs,
                                   float[] ys,
                                   float[] zs,
                                   float[] scaleXs,
                                   float[] scaleYs,
                                   float[] rotationDegs,
                                   float[] animationStates,
                                   BlendMode[] blendModes) {
        requirePayloadField(textureSlots, "textureSlots");
        return ofNativePayload(textureSlots,
                nativeBlendModes(blendModes, textureSlots.length),
                defaultIntValues(textureSlots.length),
                defaultIntValues(textureSlots.length),
                defaultIntValues(textureSlots.length),
                defaultIntValues(textureSlots.length),
                defaultIntValues(textureSlots.length),
                defaultIntValues(textureSlots.length),
                defaultFloatValues(textureSlots.length, 0.0f),
                defaultFloatValues(textureSlots.length, 0.0f),
                defaultFloatValues(textureSlots.length, 1.0f),
                defaultFloatValues(textureSlots.length, 1.0f),
                xs,
                ys,
                zs,
                scaleXs,
                scaleYs,
                rotationDegs,
                animationStates);
    }

    public static SceneSnapshot of(int[] textureSlots,
                                   float[] xs,
                                   float[] ys,
                                   float[] zs,
                                   float[] scaleXs,
                                   float[] scaleYs,
                                   float[] rotationDegs,
                                   float[] animationStates,
                                   BlendMode[] blendModes,
                                   int[] layers,
                                   int[] renderOrders) {
        requirePayloadField(textureSlots, "textureSlots");
        return ofNativePayload(textureSlots,
                nativeBlendModes(blendModes, textureSlots.length),
                layers,
                renderOrders,
                defaultIntValues(textureSlots.length),
                defaultIntValues(textureSlots.length),
                defaultIntValues(textureSlots.length),
                defaultIntValues(textureSlots.length),
                defaultFloatValues(textureSlots.length, 0.0f),
                defaultFloatValues(textureSlots.length, 0.0f),
                defaultFloatValues(textureSlots.length, 1.0f),
                defaultFloatValues(textureSlots.length, 1.0f),
                xs,
                ys,
                zs,
                scaleXs,
                scaleYs,
                rotationDegs,
                animationStates);
    }

    public static SceneSnapshot of(int[] textureSlots,
                                   float[] xs,
                                   float[] ys,
                                   float[] zs,
                                   float[] scaleXs,
                                   float[] scaleYs,
                                   float[] rotationDegs,
                                   float[] animationStates,
                                   BlendMode[] blendModes,
                                   int[] layers,
                                   int[] renderOrders,
                                   ScissorRect[] scissors) {
        requirePayloadField(textureSlots, "textureSlots");
        NativeScissors nativeScissors = nativeScissors(scissors, textureSlots.length);
        return ofNativePayload(textureSlots,
                nativeBlendModes(blendModes, textureSlots.length),
                layers,
                renderOrders,
                nativeScissors.xs,
                nativeScissors.ys,
                nativeScissors.widths,
                nativeScissors.heights,
                defaultFloatValues(textureSlots.length, 0.0f),
                defaultFloatValues(textureSlots.length, 0.0f),
                defaultFloatValues(textureSlots.length, 1.0f),
                defaultFloatValues(textureSlots.length, 1.0f),
                xs,
                ys,
                zs,
                scaleXs,
                scaleYs,
                rotationDegs,
                animationStates);
    }

    public static SceneSnapshot of(int[] textureSlots,
                                   float[] xs,
                                   float[] ys,
                                   float[] zs,
                                   float[] scaleXs,
                                   float[] scaleYs,
                                   float[] rotationDegs,
                                   float[] animationStates,
                                   BlendMode[] blendModes,
                                   int[] layers,
                                   int[] renderOrders,
                                   ScissorRect[] scissors,
                                   TextureRegion[] textureRegions) {
        requirePayloadField(textureSlots, "textureSlots");
        NativeScissors nativeScissors = nativeScissors(scissors, textureSlots.length);
        NativeTextureRegions nativeTextureRegions = nativeTextureRegions(textureRegions, textureSlots.length);
        return ofNativePayload(textureSlots,
                nativeBlendModes(blendModes, textureSlots.length),
                layers,
                renderOrders,
                nativeScissors.xs,
                nativeScissors.ys,
                nativeScissors.widths,
                nativeScissors.heights,
                nativeTextureRegions.us,
                nativeTextureRegions.vs,
                nativeTextureRegions.widthUvs,
                nativeTextureRegions.heightUvs,
                xs,
                ys,
                zs,
                scaleXs,
                scaleYs,
                rotationDegs,
                animationStates);
    }

    private static SceneSnapshot ofNativePayload(int[] textureSlots,
                                                 int[] blendModes,
                                                 int[] layers,
                                                 int[] renderOrders,
                                                 int[] scissorXs,
                                                 int[] scissorYs,
                                                 int[] scissorWidths,
                                                 int[] scissorHeights,
                                                 float[] textureUs,
                                                 float[] textureVs,
                                                 float[] textureWidthUvs,
                                                 float[] textureHeightUvs,
                                                 float[] xs,
                                                 float[] ys,
                                                 float[] zs,
                                                 float[] scaleXs,
                                                 float[] scaleYs,
                                                 float[] rotationDegs,
                                                 float[] animationStates) {
        requirePayloadField(textureSlots, "textureSlots");
        requirePayloadField(blendModes, "blendModes");
        requirePayloadField(layers, "layers");
        requirePayloadField(renderOrders, "renderOrders");
        requirePayloadField(scissorXs, "scissorXs");
        requirePayloadField(scissorYs, "scissorYs");
        requirePayloadField(scissorWidths, "scissorWidths");
        requirePayloadField(scissorHeights, "scissorHeights");
        requirePayloadField(textureUs, "textureUs");
        requirePayloadField(textureVs, "textureVs");
        requirePayloadField(textureWidthUvs, "textureWidthUvs");
        requirePayloadField(textureHeightUvs, "textureHeightUvs");
        requirePayloadField(xs, "xs");
        requirePayloadField(ys, "ys");
        requirePayloadField(zs, "zs");
        requirePayloadField(scaleXs, "scaleXs");
        requirePayloadField(scaleYs, "scaleYs");
        requirePayloadField(rotationDegs, "rotationDegs");
        requirePayloadField(animationStates, "animationStates");

        int size = textureSlots.length;
        SceneSubmissionContract.validateSpriteCount(size);
        validatePayloadLength("blendModes", blendModes.length, size);
        validatePayloadLength("layers", layers.length, size);
        validatePayloadLength("renderOrders", renderOrders.length, size);
        validatePayloadLength("scissorXs", scissorXs.length, size);
        validatePayloadLength("scissorYs", scissorYs.length, size);
        validatePayloadLength("scissorWidths", scissorWidths.length, size);
        validatePayloadLength("scissorHeights", scissorHeights.length, size);
        validatePayloadLength("textureUs", textureUs.length, size);
        validatePayloadLength("textureVs", textureVs.length, size);
        validatePayloadLength("textureWidthUvs", textureWidthUvs.length, size);
        validatePayloadLength("textureHeightUvs", textureHeightUvs.length, size);
        validatePayloadLength("xs", xs.length, size);
        validatePayloadLength("ys", ys.length, size);
        validatePayloadLength("zs", zs.length, size);
        validatePayloadLength("scaleXs", scaleXs.length, size);
        validatePayloadLength("scaleYs", scaleYs.length, size);
        validatePayloadLength("rotationDegs", rotationDegs.length, size);
        validatePayloadLength("animationStates", animationStates.length, size);
        validateTextureSlots("textureSlots", textureSlots);
        validateNativeBlendModes("blendModes", blendModes);
        validateFiniteValues("textureUs", textureUs);
        validateFiniteValues("textureVs", textureVs);
        validateFiniteValues("textureWidthUvs", textureWidthUvs);
        validateFiniteValues("textureHeightUvs", textureHeightUvs);
        validateFiniteValues("xs", xs);
        validateFiniteValues("ys", ys);
        validateFiniteValues("zs", zs);
        validateFiniteValues("scaleXs", scaleXs);
        validateFiniteValues("scaleYs", scaleYs);
        validateFiniteValues("rotationDegs", rotationDegs);
        validateFiniteValues("animationStates", animationStates);
        if (size == 0) {
            return EMPTY;
        }
        return new SceneSnapshot(textureSlots,
                blendModes,
                layers,
                renderOrders,
                scissorXs,
                scissorYs,
                scissorWidths,
                scissorHeights,
                textureUs,
                textureVs,
                textureWidthUvs,
                textureHeightUvs,
                xs,
                ys,
                zs,
                scaleXs,
                scaleYs,
                rotationDegs,
                animationStates,
                true);
    }

    private static int[] defaultIntValues(int size) {
        return new int[size];
    }

    private static int[] defaultBlendModes(int size) {
        int[] blendModes = new int[size];
        Arrays.fill(blendModes, BlendMode.ALPHA.nativeValue());
        return blendModes;
    }

    private static float[] defaultFloatValues(int size, float value) {
        float[] values = new float[size];
        Arrays.fill(values, value);
        return values;
    }

    private static <T> T requirePayloadField(T value, String fieldName) {
        return Objects.requireNonNull(value, fieldName + " must not be null");
    }

    private static void validatePayloadLength(String fieldName, int actualLength, int expectedLength) {
        SceneSubmissionContract.validateLength(fieldName, actualLength, expectedLength);
    }

    private static void validateTextureSlots(String fieldName, int[] textureSlots) {
        for (int i = 0; i < textureSlots.length; i++) {
            SceneSubmissionContract.validateTextureSlot(fieldName, i, textureSlots[i]);
        }
    }

    private static void validateNativeBlendModes(String fieldName, int[] blendModes) {
        for (int i = 0; i < blendModes.length; i++) {
            if (!BlendMode.isValidNativeValue(blendModes[i])) {
                throw new IllegalArgumentException("SceneSnapshot " + fieldName + "[" + i +
                        "] has unsupported native value " + blendModes[i] +
                        " (expected one of " + BlendMode.describeNativeValues() + ")");
            }
        }
    }

    private static void validateFiniteValues(String fieldName, float[] values) {
        for (int i = 0; i < values.length; i++) {
            if (!Float.isFinite(values[i])) {
                throw new IllegalArgumentException("SceneSnapshot " + fieldName + "[" + i +
                        "] must be finite but was " + values[i]);
            }
        }
    }

    private static int[] nativeBlendModes(BlendMode[] blendModes, int expectedSize) {
        requirePayloadField(blendModes, "blendModes");
        validatePayloadLength("blendModes", blendModes.length, expectedSize);
        int[] nativeBlendModes = new int[blendModes.length];
        for (int i = 0; i < blendModes.length; i++) {
            BlendMode blendMode = Objects.requireNonNull(blendModes[i], "blendModes[" + i + "] must not be null");
            nativeBlendModes[i] = blendMode.nativeValue();
        }
        return nativeBlendModes;
    }

    private static NativeScissors nativeScissors(ScissorRect[] scissors, int expectedSize) {
        requirePayloadField(scissors, "scissors");
        validatePayloadLength("scissors", scissors.length, expectedSize);

        NativeScissors nativeScissors = new NativeScissors(expectedSize);
        for (int i = 0; i < scissors.length; i++) {
            ScissorRect scissor = Objects.requireNonNull(scissors[i], "scissors[" + i + "] must not be null");
            if (!scissor.isEnabled()) {
                continue;
            }
            nativeScissors.xs[i] = scissor.getX();
            nativeScissors.ys[i] = scissor.getY();
            nativeScissors.widths[i] = scissor.getWidth();
            nativeScissors.heights[i] = scissor.getHeight();
        }
        return nativeScissors;
    }

    private static NativeTextureRegions nativeTextureRegions(TextureRegion[] textureRegions, int expectedSize) {
        requirePayloadField(textureRegions, "textureRegions");
        validatePayloadLength("textureRegions", textureRegions.length, expectedSize);

        NativeTextureRegions nativeTextureRegions = new NativeTextureRegions(expectedSize);
        for (int i = 0; i < textureRegions.length; i++) {
            TextureRegion textureRegion =
                    Objects.requireNonNull(textureRegions[i], "textureRegions[" + i + "] must not be null");
            nativeTextureRegions.us[i] = textureRegion.getU();
            nativeTextureRegions.vs[i] = textureRegion.getV();
            nativeTextureRegions.widthUvs[i] = textureRegion.getWidthUv();
            nativeTextureRegions.heightUvs[i] = textureRegion.getHeightUv();
        }
        return nativeTextureRegions;
    }

    private static final class NativeScissors {
        private final int[] xs;
        private final int[] ys;
        private final int[] widths;
        private final int[] heights;

        private NativeScissors(int size) {
            this.xs = new int[size];
            this.ys = new int[size];
            this.widths = new int[size];
            this.heights = new int[size];
        }
    }

    private static final class NativeTextureRegions {
        private final float[] us;
        private final float[] vs;
        private final float[] widthUvs;
        private final float[] heightUvs;

        private NativeTextureRegions(int size) {
            this.us = new float[size];
            this.vs = new float[size];
            this.widthUvs = new float[size];
            this.heightUvs = new float[size];
        }
    }

    public int size() {
        return textureSlots.length;
    }

    int[] textureSlotsRaw() {
        return textureSlots;
    }

    int[] blendModesRaw() {
        return blendModes;
    }

    int[] layersRaw() {
        return layers;
    }

    int[] renderOrdersRaw() {
        return renderOrders;
    }

    int[] scissorXsRaw() {
        return scissorXs;
    }

    int[] scissorYsRaw() {
        return scissorYs;
    }

    int[] scissorWidthsRaw() {
        return scissorWidths;
    }

    int[] scissorHeightsRaw() {
        return scissorHeights;
    }

    float[] textureUsRaw() {
        return textureUs;
    }

    float[] textureVsRaw() {
        return textureVs;
    }

    float[] textureWidthUvsRaw() {
        return textureWidthUvs;
    }

    float[] textureHeightUvsRaw() {
        return textureHeightUvs;
    }

    float[] xsRaw() {
        return xs;
    }

    float[] ysRaw() {
        return ys;
    }

    float[] zsRaw() {
        return zs;
    }

    float[] scaleXsRaw() {
        return scaleXs;
    }

    float[] scaleYsRaw() {
        return scaleYs;
    }

    float[] rotationDegsRaw() {
        return rotationDegs;
    }

    float[] animationStatesRaw() {
        return animationStates;
    }
}

