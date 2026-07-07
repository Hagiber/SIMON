package com.hsw.etalonapp.engine.api;

public final class SceneSubmissionContract {

    public static final int PAYLOAD_VERSION = 1;
    public static final int MAX_SPRITES = 4096;
    public static final int MAX_TEXTURE_SLOT = 1023;

    private SceneSubmissionContract() {
    }

    static void validateSpriteCount(int spriteCount) {
        if (spriteCount > MAX_SPRITES) {
            throw new IllegalArgumentException("SceneSnapshot sprite count " + spriteCount +
                    " exceeds max " + MAX_SPRITES);
        }
    }

    static void validateLength(String fieldName, int actualLength, int expectedLength) {
        if (actualLength != expectedLength) {
            throw new IllegalArgumentException("SceneSnapshot field " + fieldName +
                    " length " + actualLength +
                    " does not match textureSlots length " + expectedLength);
        }
    }

    static void validateTextureSlot(String fieldName, int index, int textureSlot) {
        if (textureSlot < 0 || textureSlot > MAX_TEXTURE_SLOT) {
            throw new IllegalArgumentException("SceneSnapshot " + fieldName + "[" + index +
                    "] must be between 0 and " + MAX_TEXTURE_SLOT +
                    " but was " + textureSlot);
        }
    }
}
