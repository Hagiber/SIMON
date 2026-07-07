package com.hsw.simonapp.engine.assets;

import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;

import com.hsw.simonapp.engine.scene.model.TextureHandle;
import com.hsw.simonapp.engine.api.NativeLib;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Map;

public final class TextureAssetLoader {

    private static final int NATIVE_SLOT_BASE = 1;

    private final AssetManager assetManager;
    private final NativeLib nativeLib;
    private final Map<TextureCacheKey, TextureHandle> handleCache = new HashMap<>();

    public TextureAssetLoader(AssetManager assetManager, NativeLib nativeLib) {
        this.assetManager = assetManager;
        this.nativeLib = nativeLib;
    }

    public TextureUploadResult uploadTexture(String assetName, float alphaBlend) {
        float normalizedAlpha = clamp(alphaBlend, 0.0f, 1.0f);
        TextureCacheKey cacheKey = new TextureCacheKey(assetName, normalizedAlpha);
        TextureHandle cachedHandle = handleCache.get(cacheKey);
        if (cachedHandle != null) {
            return TextureUploadResult.success(cachedHandle, "Texture returned from cache");
        }

        try (InputStream inputStream = assetManager.open(assetName)) {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;
            options.inPremultiplied = false;
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream, null, options);
            if (bitmap == null) {
                return TextureUploadResult.failure(TextureUploadErrorCode.BITMAP_DECODE_FAILED,
                        "Failed to decode bitmap from asset: " + assetName);
            }

            Bitmap rgbaBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
            if (rgbaBitmap == null) {
                bitmap.recycle();
                return TextureUploadResult.failure(TextureUploadErrorCode.BITMAP_COPY_FAILED,
                        "Failed to create ARGB_8888 bitmap copy for asset: " + assetName);
            }
            if (rgbaBitmap != bitmap) {
                bitmap.recycle();
            }
            rgbaBitmap.setPremultiplied(false);

            applyAlphaToBitmap(rgbaBitmap, normalizedAlpha);
            rgbaBitmap.setPremultiplied(false);
            long byteCountLong = (long) rgbaBitmap.getWidth() * (long) rgbaBitmap.getHeight() * 4L;
            if (byteCountLong > Integer.MAX_VALUE) {
                rgbaBitmap.recycle();
                return TextureUploadResult.failure(TextureUploadErrorCode.TEXTURE_TOO_LARGE,
                        "Texture is too large to upload: " + assetName);
            }
            int byteCount = (int) byteCountLong;
            ByteBuffer raw = ByteBuffer.allocateDirect(byteCount).order(ByteOrder.nativeOrder());
            rgbaBitmap.copyPixelsToBuffer(raw);
            raw.rewind();

            int slotResult = nativeLib.uploadTextureRgba(assetName,
                    rgbaBitmap.getWidth(),
                    rgbaBitmap.getHeight(),
                    raw);
            rgbaBitmap.recycle();

            int validatedSlot = toZeroBasedSlot(slotResult);
            if (validatedSlot < 0) {
                return TextureUploadResult.failure(TextureUploadErrorCode.NATIVE_UPLOAD_FAILED,
                        "Native uploadTextureRgba returned invalid slot " + slotResult + " for asset: " + assetName);
            }
            TextureHandle textureHandle = TextureHandle.of(validatedSlot);
            handleCache.put(cacheKey, textureHandle);
            return TextureUploadResult.success(textureHandle, "Texture queued for streaming upload");
        } catch (IOException e) {
            return TextureUploadResult.failure(TextureUploadErrorCode.ASSET_READ_FAILED,
                    "Failed to open texture asset: " + assetName + " (" + e.getMessage() + ")");
        }
    }

    public void clearCache() {
        handleCache.clear();
        nativeLib.clearTextureCache();
    }

    public boolean releaseTexture(TextureHandle textureHandle) {
        if (textureHandle == null) {
            return false;
        }

        boolean nativeReleased = nativeLib.releaseTexture(textureHandle.value());
        if (nativeReleased) {
            handleCache.values().removeIf(textureHandle::equals);
        }
        return nativeReleased;
    }

    private int toZeroBasedSlot(int nativeSlotResult) {
        if (nativeSlotResult < NATIVE_SLOT_BASE) {
            return -1;
        }
        int zeroBasedSlot = nativeSlotResult - NATIVE_SLOT_BASE;
        if (zeroBasedSlot < 0) {
            return -1;
        }
        return zeroBasedSlot;
    }

    private void applyAlphaToBitmap(Bitmap bitmap, float alphaBlend) {
        float clampedAlpha = clamp(alphaBlend, 0.0f, 1.0f);
        if (clampedAlpha >= 0.999f) {
            return;
        }

        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int[] pixels = new int[width * height];
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height);
        for (int i = 0; i < pixels.length; i++) {
            int pixel = pixels[i];
            int newAlpha = Math.round(Color.alpha(pixel) * clampedAlpha);
            pixels[i] = Color.argb(newAlpha, Color.red(pixel), Color.green(pixel), Color.blue(pixel));
        }
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
    }

    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private static final class TextureCacheKey {
        private final String assetName;
        private final int alphaBits;

        private TextureCacheKey(String assetName, float normalizedAlpha) {
            this.assetName = assetName;
            this.alphaBits = Float.floatToIntBits(normalizedAlpha);
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof TextureCacheKey)) {
                return false;
            }
            TextureCacheKey that = (TextureCacheKey) other;
            return alphaBits == that.alphaBits && assetName.equals(that.assetName);
        }

        @Override
        public int hashCode() {
            int result = assetName.hashCode();
            result = 31 * result + alphaBits;
            return result;
        }
    }

    public enum TextureUploadErrorCode {
        ASSET_READ_FAILED,
        BITMAP_DECODE_FAILED,
        BITMAP_COPY_FAILED,
        TEXTURE_TOO_LARGE,
        NATIVE_UPLOAD_FAILED
    }

    public static final class TextureUploadResult {
        private final boolean success;
        private final TextureHandle textureHandle;
        private final TextureUploadErrorCode errorCode;
        private final String message;

        private TextureUploadResult(boolean success, TextureHandle textureHandle, TextureUploadErrorCode errorCode, String message) {
            this.success = success;
            this.textureHandle = textureHandle;
            this.errorCode = errorCode;
            this.message = message;
        }

        public static TextureUploadResult success(TextureHandle textureHandle, String message) {
            return new TextureUploadResult(true, textureHandle, null, message);
        }

        public static TextureUploadResult failure(TextureUploadErrorCode errorCode, String message) {
            return new TextureUploadResult(false, null, errorCode, message);
        }

        public boolean isSuccess() {
            return success;
        }

        public TextureHandle getTextureHandle() {
            return textureHandle;
        }

        public TextureUploadErrorCode getErrorCode() {
            return errorCode;
        }

        public String getMessage() {
            return message;
        }
    }
}
