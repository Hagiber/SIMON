package com.hsw.vulkansmokehost;

import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Objects;

final class SmokeTextureAssetLoader {

    private static final int NATIVE_SLOT_BASE = 1;

    private final AssetManager assetManager;
    private final SmokeSampleRenderer renderer;

    SmokeTextureAssetLoader(AssetManager assetManager, SmokeSampleRenderer renderer) {
        this.assetManager = Objects.requireNonNull(assetManager, "assetManager");
        this.renderer = Objects.requireNonNull(renderer, "renderer");
    }

    TextureUploadResult uploadTextureToSlot(String assetName, int textureSlot) {
        if (textureSlot < 0) {
            return TextureUploadResult.failure("Texture slot must be non-negative, got: " + textureSlot);
        }

        try (InputStream inputStream = assetManager.open(assetName)) {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;
            options.inPremultiplied = false;
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream, null, options);
            if (bitmap == null) {
                return TextureUploadResult.failure("Failed to decode bitmap from asset: " + assetName);
            }

            Bitmap rgbaBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
            if (rgbaBitmap == null) {
                bitmap.recycle();
                return TextureUploadResult.failure("Failed to create ARGB_8888 bitmap copy for asset: " + assetName);
            }
            if (rgbaBitmap != bitmap) {
                bitmap.recycle();
            }
            rgbaBitmap.setPremultiplied(false);

            long byteCountLong = (long) rgbaBitmap.getWidth() * (long) rgbaBitmap.getHeight() * 4L;
            if (byteCountLong > Integer.MAX_VALUE) {
                rgbaBitmap.recycle();
                return TextureUploadResult.failure("Texture is too large to upload: " + assetName);
            }

            ByteBuffer raw = ByteBuffer.allocateDirect((int) byteCountLong).order(ByteOrder.nativeOrder());
            rgbaBitmap.copyPixelsToBuffer(raw);
            raw.rewind();

            int slotResult = renderer.uploadTextureRgbaToSlot(assetName,
                    rgbaBitmap.getWidth(),
                    rgbaBitmap.getHeight(),
                    raw,
                    textureSlot);
            rgbaBitmap.recycle();

            int validatedSlot = toZeroBasedSlot(slotResult);
            if (validatedSlot != textureSlot) {
                return TextureUploadResult.failure("Renderer uploadTextureRgbaToSlot returned invalid slot "
                        + slotResult + " for asset: " + assetName);
            }

            return TextureUploadResult.success();
        } catch (IOException e) {
            return TextureUploadResult.failure("Failed to open texture asset: "
                    + assetName + " (" + e.getMessage() + ")");
        }
    }

    private int toZeroBasedSlot(int nativeSlotResult) {
        if (nativeSlotResult < NATIVE_SLOT_BASE) {
            return -1;
        }
        return nativeSlotResult - NATIVE_SLOT_BASE;
    }

    static final class TextureUploadResult {
        private final boolean success;
        private final String message;

        private TextureUploadResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        static TextureUploadResult success() {
            return new TextureUploadResult(true, "Texture queued for streaming upload");
        }

        static TextureUploadResult failure(String message) {
            return new TextureUploadResult(false, message);
        }

        boolean isSuccess() {
            return success;
        }

        String getMessage() {
            return message;
        }
    }
}
