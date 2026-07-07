package com.hsw.etalonapp.engine.scene;

import com.hsw.etalonapp.engine.scene.model.TextureSpec;
import com.hsw.etalonapp.engine.scene.model.RgbColor;
import com.hsw.etalonapp.engine.scene.model.ShapeFillMode;
import com.hsw.etalonapp.engine.scene.model.ShapeTextureRegistration;
import com.hsw.etalonapp.engine.scene.model.ShapeTextureSpec;

import java.util.Arrays;
import java.util.List;

public final class TextureCatalog {

    public static final String FIRE_ATLAS_METADATA_ASSET = "atlases/firesheet5x5.atlas.json";
    public static final String FIRE_ATLAS_IMAGE_ASSET = "atlases/firesheet5x5.atlas.png";
    public static final int DOLPHIN_TEXTURE_SLOT = 0;
    public static final int PING_TEXTURE_SLOT = 1;
    public static final int WIFI_TEXTURE_SLOT = 2;
    public static final int FIRE_ATLAS_TEXTURE_SLOT = 3;
    public static final int LINE_TEXTURE_SLOT = 4;
    public static final int ELLIPSE_TEXTURE_SLOT = 5;
    public static final int SQUARE_TEXTURE_SLOT = 6;

    private TextureCatalog() {
    }

    public static List<TextureSpec> defaultScene() {
        return Arrays.asList(
                new TextureSpec("dolphin.png", 0.0f, 0.0f, 1.0f, 15.0f, 1.00f),
                new TextureSpec("ping.png", 0.0f, -0.0f, 0.5f, -12.0f, 1.00f),
                new TextureSpec("wifi.png", 0.0f, -0.0f, 0.4f, 0.0f, 1.00f),
                new TextureSpec(FIRE_ATLAS_IMAGE_ASSET, 0.00f, 0.0f, 0.6f, 0.0f, 1.00f)
        );
    }

    public static List<ShapeTextureRegistration> defaultShapeTextures() {
        return Arrays.asList(
                new ShapeTextureRegistration(LINE_TEXTURE_SLOT,
                        ShapeTextureSpec.line("shape-line-red",
                                192,
                                10,
                                RgbColor.of(255, 72, 64))),
                new ShapeTextureRegistration(ELLIPSE_TEXTURE_SLOT,
                        ShapeTextureSpec.ellipse("shape-ellipse-filled-green",
                                112,
                                72,
                                ShapeFillMode.FILLED,
                                5,
                                RgbColor.of(72, 218, 145))),
                new ShapeTextureRegistration(SQUARE_TEXTURE_SLOT,
                        ShapeTextureSpec.square("shape-square-outline-blue",
                                86,
                                ShapeFillMode.OUTLINE,
                                8,
                                RgbColor.of(78, 168, 255)))
        );
    }
}
