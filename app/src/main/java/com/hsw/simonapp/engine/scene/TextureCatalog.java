package com.hsw.simonapp.engine.scene;

import com.hsw.simonapp.engine.scene.model.TextureSpec;
import com.hsw.simonapp.engine.scene.model.ShapeTextureRegistration;
import com.hsw.simonapp.engine.scene.layout.TextureDimensions;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class TextureCatalog {

    public static final String FIRE_ATLAS_METADATA_ASSET = "atlases/firesheet5x5.atlas.json";
    public static final String FIRE_ATLAS_IMAGE_ASSET = "atlases/firesheet5x5.atlas.png";
    public static final String RED_BUTTON_IMAGE_ASSET = "RED_button.png";
    public static final String RED_BUTTON_PRESS_ATLAS_ASSET = "RED_button_press4x3.png";
    public static final int RED_BUTTON_PRESS_ATLAS_COLUMNS = 4;
    public static final int RED_BUTTON_PRESS_ATLAS_ROWS = 3;
    public static final int RED_BUTTON_PRESS_ATLAS_FRAME_COUNT = 11;
    public static final int SIMONFRAME_TEXTURE_SLOT = 0;
    public static final int RED_BUTTON_TEXTURE_SLOT = 1;
    public static final int FIRE_ATLAS_TEXTURE_SLOT = 2;

    private TextureCatalog() {
    }

    public static List<TextureSpec> defaultScene() {
        return Arrays.asList(
                new TextureSpec("SIMON_frame.png", 0.0f, 0.0f, 0.4f, 0.0f, 1.00f),
                new TextureSpec(RED_BUTTON_PRESS_ATLAS_ASSET, 0.0f, 0.0f, 0.5f, 0.0f, 1.00f)
        );
    }

    public static List<ShapeTextureRegistration> defaultShapeTextures() {
        return Collections.emptyList();
    }

    public static TextureDimensions textureDimensionsForSlot(int textureSlot) {
        if (textureSlot == SIMONFRAME_TEXTURE_SLOT) {
            return TextureDimensions.of(168, 62);
        }
        if (textureSlot == RED_BUTTON_TEXTURE_SLOT) {
            return TextureDimensions.of(455, 455);
        }
        throw new IllegalArgumentException("Unknown texture slot: " + textureSlot);
    }
}
