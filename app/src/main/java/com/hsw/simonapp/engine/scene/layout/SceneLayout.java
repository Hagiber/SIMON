package com.hsw.simonapp.engine.scene.layout;

import com.hsw.simonapp.engine.api.OrthoCamera;
import com.hsw.simonapp.engine.loop.defaults.SimpleWorldState;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class SceneLayout {
    public static final float DEFAULT_SPRITE_EXTENT = 0.7f;

    private final TextureDimensionLookup textureDimensionLookup;
    private final List<SpriteLayoutRule> rules;

    public SceneLayout(TextureDimensionLookup textureDimensionLookup,
                       List<SpriteLayoutRule> rules) {
        this.textureDimensionLookup = Objects.requireNonNull(textureDimensionLookup, "textureDimensionLookup");
        this.rules = new ArrayList<>(Objects.requireNonNull(rules, "rules"));
    }

    public void apply(SimpleWorldState worldState, int viewportWidth, int viewportHeight) {
        apply(worldState, viewportWidth, viewportHeight, OrthoCamera.defaults());
    }

    public void apply(SimpleWorldState worldState,
                      int viewportWidth,
                      int viewportHeight,
                      OrthoCamera camera) {
        Objects.requireNonNull(worldState, "worldState");
        if (viewportWidth <= 0 || viewportHeight <= 0) {
            return;
        }
        Objects.requireNonNull(camera, "camera");

        for (SpriteLayoutRule rule : rules) {
            TextureDimensions textureDimensions =
                    textureDimensionLookup.getTextureDimensions(rule.getTextureSlot());
            SpriteLayout spriteLayout = calculateSpriteLayout(viewportWidth,
                    viewportHeight,
                    textureDimensions,
                    rule.getFitMode(),
                    rule.getFillRatio(),
                    camera).multiply(rule.getScaleXMultiplier(), rule.getScaleYMultiplier());
            applyToEntitiesWithTextureSlot(worldState, rule.getTextureSlot(), spriteLayout);
        }
    }

    public static SpriteLayout calculateSpriteLayout(int viewportWidth,
                                                     int viewportHeight,
                                                     TextureDimensions textureDimensions,
                                                     FitMode fitMode,
                                                     float fillRatio) {
        return calculateSpriteLayout(viewportWidth,
                viewportHeight,
                textureDimensions,
                fitMode,
                fillRatio,
                OrthoCamera.defaults());
    }

    public static SpriteLayout calculateSpriteLayout(int viewportWidth,
                                                     int viewportHeight,
                                                     TextureDimensions textureDimensions,
                                                     FitMode fitMode,
                                                     float fillRatio,
                                                     OrthoCamera camera) {
        if (viewportWidth <= 0 || viewportHeight <= 0) {
            throw new IllegalArgumentException("Viewport dimensions must be positive");
        }
        Objects.requireNonNull(textureDimensions, "textureDimensions");
        Objects.requireNonNull(fitMode, "fitMode");
        Objects.requireNonNull(camera, "camera");
        if (!Float.isFinite(fillRatio) || fillRatio <= 0.0f) {
            throw new IllegalArgumentException("fillRatio must be positive and finite");
        }

        ViewportWorldMetrics metrics = ViewportWorldMetrics.fromViewport(viewportWidth, viewportHeight);
        float visibleWorldWidth = metrics.visibleWorldWidth(camera);
        float visibleWorldHeight = metrics.visibleWorldHeight(camera);
        float naturalWidth = naturalSpriteWidth(textureDimensions);
        float naturalHeight = naturalSpriteHeight(textureDimensions);
        float shortSide = metrics.shortSide(camera);

        float scale;
        switch (fitMode) {
            case FIT_SHORT_SIDE:
                scale = shortSide / Math.max(naturalWidth, naturalHeight);
                break;
            case FIT_WIDTH:
                scale = visibleWorldWidth / naturalWidth;
                break;
            case FIT_HEIGHT:
                scale = visibleWorldHeight / naturalHeight;
                break;
            case CONTAIN:
                scale = Math.min(visibleWorldWidth / naturalWidth,
                        visibleWorldHeight / naturalHeight);
                break;
            case COVER:
                scale = Math.max(visibleWorldWidth / naturalWidth,
                        visibleWorldHeight / naturalHeight);
                break;
            default:
                throw new IllegalArgumentException("Unsupported fit mode: " + fitMode);
        }
        return SpriteLayout.uniform(scale * fillRatio);
    }

    private static void applyToEntitiesWithTextureSlot(SimpleWorldState worldState,
                                                       int textureSlot,
                                                       SpriteLayout spriteLayout) {
        for (SimpleWorldState.EntityState entity : worldState.getEntities()) {
            if (entity.getTextureSlot() == textureSlot) {
                entity.setScaleX(spriteLayout.getScaleX());
                entity.setScaleY(spriteLayout.getScaleY());
            }
        }
    }

    private static float naturalSpriteWidth(TextureDimensions textureDimensions) {
        float textureWidth = textureDimensions.getWidth();
        float textureHeight = textureDimensions.getHeight();
        if (textureWidth >= textureHeight) {
            return DEFAULT_SPRITE_EXTENT;
        }
        return DEFAULT_SPRITE_EXTENT * textureWidth / textureHeight;
    }

    private static float naturalSpriteHeight(TextureDimensions textureDimensions) {
        float textureWidth = textureDimensions.getWidth();
        float textureHeight = textureDimensions.getHeight();
        if (textureHeight >= textureWidth) {
            return DEFAULT_SPRITE_EXTENT;
        }
        return DEFAULT_SPRITE_EXTENT * textureHeight / textureWidth;
    }
}
