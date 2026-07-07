package com.hsw.simonapp.engine.game;

import com.hsw.simonapp.engine.api.BlendMode;
import com.hsw.simonapp.engine.api.ScissorRect;
import com.hsw.simonapp.engine.api.TextureRegion;
import com.hsw.simonapp.engine.loop.core.WorldState;
import com.hsw.simonapp.engine.loop.defaults.SimpleWorldState;
import com.hsw.simonapp.engine.scene.TextureCatalog;
import com.hsw.simonapp.engine.scene.model.ShapeTextureRegistration;
import com.hsw.simonapp.engine.scene.model.TextureSpec;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

final class DefaultSceneDefinition implements SceneDefinition {

    private final TextureRegion fireAtlasRegion;
    private final List<TextureSpec> textureSpecs;
    private final List<ShapeTextureRegistration> shapeTextureRegistrations;

    DefaultSceneDefinition(TextureRegion fireAtlasRegion) {
        this.fireAtlasRegion = Objects.requireNonNull(fireAtlasRegion, "fireAtlasRegion");
        this.textureSpecs = Collections.unmodifiableList(TextureCatalog.defaultScene());
        this.shapeTextureRegistrations = Collections.unmodifiableList(TextureCatalog.defaultShapeTextures());
    }

    @Override
    public List<TextureSpec> textureSpecs() {
        return textureSpecs;
    }

    @Override
    public List<ShapeTextureRegistration> shapeTextureRegistrations() {
        return shapeTextureRegistrations;
    }

    @Override
    public WorldState createInitialWorldState() {
        return new SimpleWorldState(Arrays.asList(
                new SimpleWorldState.EntityState(1, 0, SimpleWorldState.EntityState.ControlMode.HUMAN_TOUCH, TextureCatalog.DOLPHIN_TEXTURE_SLOT, -0.45f, 0.25f, 1.0f, 15.0f, 0.0f, 0.25f, 0.17f, 22.0f, 0.35f, 0.15f),
                new SimpleWorldState.EntityState(2, 1, SimpleWorldState.EntityState.ControlMode.AI, TextureCatalog.PING_TEXTURE_SLOT, 0.40f, -0.20f, 0.5f, -12.0f, 0.25f, -0.21f, 0.14f, -18.0f, 0.50f, 0.14f),
                new SimpleWorldState.EntityState(3, 1, SimpleWorldState.EntityState.ControlMode.AI, TextureCatalog.WIFI_TEXTURE_SLOT, 0.10f, -0.10f, 0.4f, 0.0f, 0.5f, 0.18f, -0.24f, 27.0f, 0.65f, 0.12f),
                new SimpleWorldState.EntityState(4,
                        1,
                        SimpleWorldState.EntityState.ControlMode.AI,
                        TextureCatalog.FIRE_ATLAS_TEXTURE_SLOT,
                        -0.05f,
                        0.52f,
                        0.6f,
                        0.0f,
                        0.25f,
                        0.10f,
                        -0.16f,
                        12.0f,
                        19.40f,
                        0.10f,
                        BlendMode.ADDITIVE,
                        1,
                        0,
                        ScissorRect.disabled(),
                        fireAtlasRegion),
                createShapeEntity(5,
                        TextureCatalog.LINE_TEXTURE_SLOT,
                        -0.55f,
                        -0.48f,
                        0.7f,
                        18.0f,
                        0.16f,
                        0.12f,
                        -26.0f,
                        0.10f,
                        0.95f,
                        1.0f,
                        1),
                createShapeEntity(6,
                        TextureCatalog.ELLIPSE_TEXTURE_SLOT,
                        0.36f,
                        0.42f,
                        0.8f,
                        -8.0f,
                        -0.13f,
                        0.11f,
                        18.0f,
                        0.13f,
                        0.58f,
                        0.58f,
                        2),
                createShapeEntity(7,
                        TextureCatalog.SQUARE_TEXTURE_SLOT,
                        0.66f,
                        -0.52f,
                        0.9f,
                        0.0f,
                        -0.18f,
                        0.10f,
                        34.0f,
                        0.12f,
                        0.46f,
                        0.46f,
                        3)
        ));
    }

    private static SimpleWorldState.EntityState createShapeEntity(int entityId,
                                                                  int textureSlot,
                                                                  float x,
                                                                  float y,
                                                                  float z,
                                                                  float rotationDeg,
                                                                  float velocityX,
                                                                  float velocityY,
                                                                  float angularVelocityDeg,
                                                                  float collisionRadius,
                                                                  float scaleX,
                                                                  float scaleY,
                                                                  int renderOrder) {
        return new SimpleWorldState.EntityState(entityId,
                1,
                SimpleWorldState.EntityState.ControlMode.AI,
                textureSlot,
                x,
                y,
                z,
                rotationDeg,
                0.0f,
                velocityX,
                velocityY,
                angularVelocityDeg,
                0.0f,
                collisionRadius,
                BlendMode.ALPHA,
                1,
                renderOrder,
                ScissorRect.disabled(),
                TextureRegion.full(),
                scaleX,
                scaleY);
    }
}
