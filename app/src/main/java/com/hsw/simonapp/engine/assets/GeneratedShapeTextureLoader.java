package com.hsw.simonapp.engine.assets;

import com.hsw.simonapp.engine.scene.model.RgbColor;
import com.hsw.simonapp.engine.scene.model.ShapeFillMode;
import com.hsw.simonapp.engine.scene.model.ShapeKind;
import com.hsw.simonapp.engine.scene.model.ShapeTextureSpec;
import com.hsw.simonapp.engine.api.NativeLib;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Objects;

public final class GeneratedShapeTextureLoader {

    private static final int NATIVE_SLOT_BASE = 1;

    private final NativeLib nativeLib;

    public GeneratedShapeTextureLoader(NativeLib nativeLib) {
        this.nativeLib = Objects.requireNonNull(nativeLib, "nativeLib");
    }

    public ShapeTextureUploadResult uploadShapeTextureToSlot(ShapeTextureSpec textureSpec, int textureSlot) {
        Objects.requireNonNull(textureSpec, "textureSpec");
        if (textureSlot < 0) {
            return ShapeTextureUploadResult.failure(ShapeTextureUploadErrorCode.INVALID_TEXTURE_SLOT,
                    "Shape texture slot must be non-negative, got: " + textureSlot);
        }

        byte[] pixels = renderRgba(textureSpec);
        ByteBuffer raw = ByteBuffer.allocateDirect(pixels.length).order(ByteOrder.nativeOrder());
        raw.put(pixels);
        raw.rewind();

        int slotResult = nativeLib.uploadTextureRgbaToSlot(textureSpec.getDebugName(),
                textureSpec.getWidth(),
                textureSpec.getHeight(),
                raw,
                textureSlot);
        int validatedSlot = toZeroBasedSlot(slotResult);
        if (validatedSlot != textureSlot) {
            return ShapeTextureUploadResult.failure(ShapeTextureUploadErrorCode.NATIVE_UPLOAD_FAILED,
                    "Native uploadTextureRgbaToSlot returned invalid slot " + slotResult
                            + " for shape texture: " + textureSpec.getDebugName());
        }

        return ShapeTextureUploadResult.success("Shape texture queued for streaming upload");
    }

    static byte[] renderRgba(ShapeTextureSpec textureSpec) {
        Objects.requireNonNull(textureSpec, "textureSpec");
        int width = textureSpec.getWidth();
        int height = textureSpec.getHeight();
        byte[] pixels = new byte[width * height * 4];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (!isShapePixel(textureSpec, x, y)) {
                    continue;
                }

                int offset = (y * width + x) * 4;
                RgbColor color = textureSpec.getColor();
                pixels[offset] = (byte) color.getRed();
                pixels[offset + 1] = (byte) color.getGreen();
                pixels[offset + 2] = (byte) color.getBlue();
                pixels[offset + 3] = (byte) 255;
            }
        }

        return pixels;
    }

    private static boolean isShapePixel(ShapeTextureSpec textureSpec, int x, int y) {
        ShapeKind kind = textureSpec.getKind();
        if (kind == ShapeKind.LINE) {
            return true;
        }
        if (kind == ShapeKind.SQUARE) {
            return isSquarePixel(textureSpec, x, y);
        }
        if (kind == ShapeKind.ELLIPSE) {
            return isEllipsePixel(textureSpec, x, y);
        }
        return false;
    }

    private static boolean isSquarePixel(ShapeTextureSpec textureSpec, int x, int y) {
        if (textureSpec.getFillMode() == ShapeFillMode.FILLED) {
            return true;
        }

        int strokeWidth = textureSpec.getStrokeWidth();
        return x < strokeWidth ||
                y < strokeWidth ||
                x >= textureSpec.getWidth() - strokeWidth ||
                y >= textureSpec.getHeight() - strokeWidth;
    }

    private static boolean isEllipsePixel(ShapeTextureSpec textureSpec, int x, int y) {
        float centerX = textureSpec.getWidth() * 0.5f;
        float centerY = textureSpec.getHeight() * 0.5f;
        float radiusX = textureSpec.getWidth() * 0.5f;
        float radiusY = textureSpec.getHeight() * 0.5f;

        float normalizedX = ((x + 0.5f) - centerX) / radiusX;
        float normalizedY = ((y + 0.5f) - centerY) / radiusY;
        boolean insideOuter = normalizedX * normalizedX + normalizedY * normalizedY <= 1.0f;
        if (!insideOuter || textureSpec.getFillMode() == ShapeFillMode.FILLED) {
            return insideOuter;
        }

        float innerRadiusX = radiusX - textureSpec.getStrokeWidth();
        float innerRadiusY = radiusY - textureSpec.getStrokeWidth();
        if (innerRadiusX <= 0.0f || innerRadiusY <= 0.0f) {
            return true;
        }

        float innerNormalizedX = ((x + 0.5f) - centerX) / innerRadiusX;
        float innerNormalizedY = ((y + 0.5f) - centerY) / innerRadiusY;
        return innerNormalizedX * innerNormalizedX + innerNormalizedY * innerNormalizedY > 1.0f;
    }

    private static int toZeroBasedSlot(int nativeSlotResult) {
        if (nativeSlotResult < NATIVE_SLOT_BASE) {
            return -1;
        }
        return nativeSlotResult - NATIVE_SLOT_BASE;
    }

    public enum ShapeTextureUploadErrorCode {
        INVALID_TEXTURE_SLOT,
        NATIVE_UPLOAD_FAILED
    }

    public static final class ShapeTextureUploadResult {
        private final boolean success;
        private final ShapeTextureUploadErrorCode errorCode;
        private final String message;

        private ShapeTextureUploadResult(boolean success,
                                         ShapeTextureUploadErrorCode errorCode,
                                         String message) {
            this.success = success;
            this.errorCode = errorCode;
            this.message = message;
        }

        public static ShapeTextureUploadResult success(String message) {
            return new ShapeTextureUploadResult(true, null, message);
        }

        public static ShapeTextureUploadResult failure(ShapeTextureUploadErrorCode errorCode, String message) {
            return new ShapeTextureUploadResult(false, errorCode, message);
        }

        public boolean isSuccess() {
            return success;
        }

        public ShapeTextureUploadErrorCode getErrorCode() {
            return errorCode;
        }

        public String getMessage() {
            return message;
        }
    }
}
