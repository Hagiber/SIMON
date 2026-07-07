package com.hsw.simonapp.engine.assets;

import static org.junit.Assert.assertEquals;

import com.hsw.simonapp.engine.scene.model.RgbColor;
import com.hsw.simonapp.engine.scene.model.ShapeFillMode;
import com.hsw.simonapp.engine.scene.model.ShapeTextureSpec;

import org.junit.Test;

public class GeneratedShapeTextureLoaderTest {

    @Test
    public void renderRgba_filledSquareUsesRequestedRgbColor() {
        ShapeTextureSpec textureSpec = ShapeTextureSpec.square("test-square",
                4,
                ShapeFillMode.FILLED,
                1,
                RgbColor.of(12, 34, 56));

        byte[] pixels = GeneratedShapeTextureLoader.renderRgba(textureSpec);

        assertPixel(pixels, 4, 2, 2, 12, 34, 56, 255);
    }

    @Test
    public void renderRgba_outlineSquareKeepsCenterTransparent() {
        ShapeTextureSpec textureSpec = ShapeTextureSpec.square("test-outline-square",
                5,
                ShapeFillMode.OUTLINE,
                1,
                RgbColor.of(200, 100, 50));

        byte[] pixels = GeneratedShapeTextureLoader.renderRgba(textureSpec);

        assertPixel(pixels, 5, 0, 0, 200, 100, 50, 255);
        assertPixel(pixels, 5, 2, 2, 0, 0, 0, 0);
    }

    @Test
    public void renderRgba_outlineEllipseKeepsCenterTransparent() {
        ShapeTextureSpec textureSpec = ShapeTextureSpec.ellipse("test-outline-ellipse",
                9,
                7,
                ShapeFillMode.OUTLINE,
                1,
                RgbColor.of(10, 220, 90));

        byte[] pixels = GeneratedShapeTextureLoader.renderRgba(textureSpec);

        assertPixel(pixels, 9, 4, 0, 10, 220, 90, 255);
        assertPixel(pixels, 9, 4, 3, 0, 0, 0, 0);
    }

    private static void assertPixel(byte[] pixels,
                                    int width,
                                    int x,
                                    int y,
                                    int red,
                                    int green,
                                    int blue,
                                    int alpha) {
        int offset = (y * width + x) * 4;
        assertEquals(red, pixels[offset] & 0xff);
        assertEquals(green, pixels[offset + 1] & 0xff);
        assertEquals(blue, pixels[offset + 2] & 0xff);
        assertEquals(alpha, pixels[offset + 3] & 0xff);
    }
}
