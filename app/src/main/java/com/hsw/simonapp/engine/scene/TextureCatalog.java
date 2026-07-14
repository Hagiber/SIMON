package com.hsw.simonapp.engine.scene;

import com.hsw.simonapp.engine.scene.model.TextureSpec;
import com.hsw.simonapp.engine.scene.model.RgbColor;
import com.hsw.simonapp.engine.scene.model.ShapeTextureRegistration;
import com.hsw.simonapp.engine.scene.model.ShapeTextureSpec;
import com.hsw.simonapp.engine.scene.layout.TextureDimensions;

import java.util.Arrays;
import java.util.List;

public final class TextureCatalog {

    public static final String FIRE_ATLAS_METADATA_ASSET = "atlases/firesheet5x5.atlas.json";
    public static final String FIRE_ATLAS_IMAGE_ASSET = "atlases/firesheet5x5.atlas.png";
    public static final int SIMONFRAME_TEXTURE_SLOT = 0;
    public static final int LINE_TEXTURE_SLOT = 1;
    public static final int FIRE_ATLAS_TEXTURE_SLOT = 2;

    private TextureCatalog() {
    }

    public static List<TextureSpec> defaultScene() {
        return Arrays.asList(
                new TextureSpec("SIMON_frame.png", 0.0f, 0.0f, 0.4f, 0.0f, 1.00f)
        );
    }

    public static List<ShapeTextureRegistration> defaultShapeTextures() {
        return Arrays.asList(
                new ShapeTextureRegistration(LINE_TEXTURE_SLOT,
                        ShapeTextureSpec.line("shape-line-red",
                                192,
                                10,
                                RgbColor.of(255, 72, 64)))
        );
    }

    public static TextureDimensions textureDimensionsForSlot(int textureSlot) {
        if (textureSlot == SIMONFRAME_TEXTURE_SLOT) {
            return TextureDimensions.of(168, 62);
        }
        if (textureSlot == LINE_TEXTURE_SLOT) {
            return TextureDimensions.of(192, 10);
        }
        throw new IllegalArgumentException("Unknown texture slot: " + textureSlot);
    }
}
