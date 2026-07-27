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
        assertEquals(TextureCatalog.BUTTON_PRESS_ATLAS_TEXTURE_SLOT, 1);
        assertEquals(TextureCatalog.BUTTON_PRESS_ATLAS_ASSET,
                textures.get(TextureCatalog.BUTTON_PRESS_ATLAS_TEXTURE_SLOT).getAssetName());
        assertEquals(TextureCatalog.RED_BUTTON_TEXTURE_SLOT, TextureCatalog.BUTTON_PRESS_ATLAS_TEXTURE_SLOT);
        assertEquals(TextureCatalog.GREEN_BUTTON_TEXTURE_SLOT, 2);
        assertEquals(TextureCatalog.BLUE_BUTTON_TEXTURE_SLOT, 3);
        assertEquals(TextureCatalog.YELLOW_BUTTON_TEXTURE_SLOT, 4);
        assertEquals(2, textures.size());
    }

    @Test
    public void buttonPressAtlas_hasSharedSequenceGrid() {
        assertEquals(8, TextureCatalog.BUTTON_PRESS_ATLAS_COLUMNS);
        assertEquals(6, TextureCatalog.BUTTON_PRESS_ATLAS_ROWS);
        assertEquals(48, TextureCatalog.BUTTON_PRESS_ATLAS_TOTAL_CELLS);
        assertEquals(11, TextureCatalog.BUTTON_PRESS_FRAME_COUNT);
    }

    @Test
    public void buttonColorIndexForSlot_mapsSimonButtonSlots() {
        assertEquals(0, TextureCatalog.buttonColorIndexForSlot(TextureCatalog.RED_BUTTON_TEXTURE_SLOT));
        assertEquals(1, TextureCatalog.buttonColorIndexForSlot(TextureCatalog.GREEN_BUTTON_TEXTURE_SLOT));
        assertEquals(2, TextureCatalog.buttonColorIndexForSlot(TextureCatalog.BLUE_BUTTON_TEXTURE_SLOT));
        assertEquals(3, TextureCatalog.buttonColorIndexForSlot(TextureCatalog.YELLOW_BUTTON_TEXTURE_SLOT));
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

    @Test
    public void textureDimensions_includeBlueButton() {
        TextureDimensions dimensions =
                TextureCatalog.textureDimensionsForSlot(TextureCatalog.BLUE_BUTTON_TEXTURE_SLOT);

        assertEquals(455, dimensions.getWidth());
        assertEquals(455, dimensions.getHeight());
    }

    @Test
    public void textureDimensions_includeYellowButton() {
        TextureDimensions dimensions =
                TextureCatalog.textureDimensionsForSlot(TextureCatalog.YELLOW_BUTTON_TEXTURE_SLOT);

        assertEquals(455, dimensions.getWidth());
        assertEquals(455, dimensions.getHeight());
    }
}
