package com.hsw.simonapp.engine.scene;

import static org.junit.Assert.assertEquals;

import com.hsw.simonapp.engine.scene.layout.TextureDimensions;
import com.hsw.simonapp.engine.scene.model.TextureSpec;

import org.junit.Test;

import java.util.List;

public class TextureCatalogTest {

    @Test
    public void defaultScene_ordersRegularTexturesByNativeSlot() {
        List<TextureSpec> textures = TextureCatalog.defaultScene();

        assertEquals(TextureCatalog.SIMONFRAME_TEXTURE_SLOT, 0);
        assertEquals("SIMON_frame.png", textures.get(TextureCatalog.SIMONFRAME_TEXTURE_SLOT).getAssetName());
        assertEquals(TextureCatalog.RED_BUTTON_TEXTURE_SLOT, 1);
        assertEquals(TextureCatalog.RED_BUTTON_PRESS_ATLAS_ASSET,
                textures.get(TextureCatalog.RED_BUTTON_TEXTURE_SLOT).getAssetName());
        assertEquals(TextureCatalog.FIRE_ATLAS_TEXTURE_SLOT, 2);
        assertEquals(TextureCatalog.FIRE_ATLAS_IMAGE_ASSET,
                textures.get(TextureCatalog.FIRE_ATLAS_TEXTURE_SLOT).getAssetName());
        assertEquals(TextureCatalog.GREEN_BUTTON_TEXTURE_SLOT, 3);
        assertEquals(TextureCatalog.GREEN_BUTTON_PRESS_ATLAS_ASSET,
                textures.get(TextureCatalog.GREEN_BUTTON_TEXTURE_SLOT).getAssetName());
    }

    @Test
    public void redButtonAtlas_hasPressSequenceGrid() {
        assertEquals(4, TextureCatalog.RED_BUTTON_PRESS_ATLAS_COLUMNS);
        assertEquals(3, TextureCatalog.RED_BUTTON_PRESS_ATLAS_ROWS);
        assertEquals(11, TextureCatalog.RED_BUTTON_PRESS_ATLAS_FRAME_COUNT);
    }

    @Test
    public void defaultShapeTextures_isEmptyWhenNoGeneratedShapesAreUsed() {
        assertEquals(0, TextureCatalog.defaultShapeTextures().size());
    }

    @Test
    public void textureDimensions_includeRedButton() {
        TextureDimensions dimensions =
                TextureCatalog.textureDimensionsForSlot(TextureCatalog.RED_BUTTON_TEXTURE_SLOT);

        assertEquals(455, dimensions.getWidth());
        assertEquals(455, dimensions.getHeight());
    }

    @Test
    public void textureDimensions_includeGreenButton() {
        TextureDimensions dimensions =
                TextureCatalog.textureDimensionsForSlot(TextureCatalog.GREEN_BUTTON_TEXTURE_SLOT);

        assertEquals(455, dimensions.getWidth());
        assertEquals(455, dimensions.getHeight());
    }
}
