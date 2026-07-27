package com.hsw.simonapp.engine.game;

import com.hsw.simonapp.engine.api.BlendMode;
import com.hsw.simonapp.engine.api.OrthoCamera;
import com.hsw.simonapp.engine.api.ScissorRect;
import com.hsw.simonapp.engine.api.TextureRegion;
import com.hsw.simonapp.engine.loop.core.WorldState;
import com.hsw.simonapp.engine.loop.defaults.SimpleWorldState;
import com.hsw.simonapp.engine.scene.TextureCatalog;
import com.hsw.simonapp.engine.scene.layout.CameraLayoutPolicy;
import com.hsw.simonapp.engine.scene.layout.FitMode;
import com.hsw.simonapp.engine.scene.layout.SceneLayout;
import com.hsw.simonapp.engine.scene.layout.SpriteLayoutRule;
import com.hsw.simonapp.engine.scene.model.ShapeTextureRegistration;
import com.hsw.simonapp.engine.scene.model.TextureSpec;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

final class DefaultSceneDefinition implements SceneDefinition {

    private static final float BUTTON_PRESS_ANIMATION_SPEED = -2.0f;

    private final List<TextureSpec> textureSpecs;
    private final List<ShapeTextureRegistration> shapeTextureRegistrations;
    private final CameraLayoutPolicy cameraLayoutPolicy;
    private final SceneLayout sceneLayout;

    DefaultSceneDefinition() {
        this.textureSpecs = Collections.unmodifiableList(TextureCatalog.defaultScene());
        this.shapeTextureRegistrations = Collections.unmodifiableList(TextureCatalog.defaultShapeTextures());
        this.cameraLayoutPolicy = new CameraLayoutPolicy(2.0f, 2.0f, FitMode.FIT_SHORT_SIDE);
        this.sceneLayout = new SceneLayout(TextureCatalog::textureDimensionsForSlot,
                Arrays.asList(new SpriteLayoutRule(TextureCatalog.SIMONFRAME_TEXTURE_SLOT,
                                FitMode.FIT_WIDTH,
                                0.96f),
                        new SpriteLayoutRule(TextureCatalog.RED_BUTTON_TEXTURE_SLOT,
                                FitMode.FIT_WIDTH,
                                0.32f,
                                1.0f,
                                0.6f),
                        new SpriteLayoutRule(TextureCatalog.GREEN_BUTTON_TEXTURE_SLOT,
                                FitMode.FIT_WIDTH,
                                0.32f,
                                1.0f,
                                0.6f),
                        new SpriteLayoutRule(TextureCatalog.BLUE_BUTTON_TEXTURE_SLOT,
                                FitMode.FIT_WIDTH,
                                0.32f,
                                1.0f,
                                0.6f),
                        new SpriteLayoutRule(TextureCatalog.YELLOW_BUTTON_TEXTURE_SLOT,
                                FitMode.FIT_WIDTH,
                                0.32f,
                                1.0f,
                                0.6f)));
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
                createSimonFrameEntity(),
                createRedButtonEntity(),
                createGreenButtonEntity(),
                createBlueButtonEntity(),
                createYellowButtonEntity()
        ));
    }

    @Override
    public OrthoCamera cameraForViewport(int viewportWidth, int viewportHeight) {
        if (viewportWidth <= 0 || viewportHeight <= 0) {
            return OrthoCamera.defaults();
        }
        return cameraLayoutPolicy.cameraForViewport(viewportWidth, viewportHeight);
    }

    @Override
    public WorldState layoutWorldState(WorldState worldState, int viewportWidth, int viewportHeight) {
        return layoutWorldState(worldState,
                viewportWidth,
                viewportHeight,
                cameraForViewport(viewportWidth, viewportHeight));
    }

    @Override
    public WorldState layoutWorldState(WorldState worldState,
                                       int viewportWidth,
                                       int viewportHeight,
                                       OrthoCamera camera) {
        if (!(worldState instanceof SimpleWorldState)) {
            return worldState;
        }
        SimpleWorldState layoutState = ((SimpleWorldState) worldState).copy();
        sceneLayout.apply(layoutState, viewportWidth, viewportHeight, camera);
        return layoutState;
    }

    private static SimpleWorldState.EntityState createSimonFrameEntity() {
        return new SimpleWorldState.EntityState(1,
                0,
                SimpleWorldState.EntityState.ControlMode.AI,
                TextureCatalog.SIMONFRAME_TEXTURE_SLOT,
                0.0f,
                -3.0f,
                1.0f,
                0.0f,
                0.0f,
                0.0f,
                0.0f,
                0.0f,
                0.0f,
                0.15f,
                BlendMode.ALPHA,
                0,
                0,
                ScissorRect.disabled(),
                TextureRegion.full(),
                1.0f,
                1.0f,
                SimpleWorldState.EntityState.TouchInteraction.NONE);
    }

    private static SimpleWorldState.EntityState createRedButtonEntity() {
        return createButtonEntity(3, TextureCatalog.RED_BUTTON_TEXTURE_SLOT, 0.33f, -1.31f);
    }

    private static SimpleWorldState.EntityState createGreenButtonEntity() {
        return createButtonEntity(4, TextureCatalog.GREEN_BUTTON_TEXTURE_SLOT, -0.30f, -1.31f);
    }

    private static SimpleWorldState.EntityState createBlueButtonEntity() {
        return createButtonEntity(5, TextureCatalog.BLUE_BUTTON_TEXTURE_SLOT, 0.33f, -0.78f);
    }

    private static SimpleWorldState.EntityState createYellowButtonEntity() {
        return createButtonEntity(6, TextureCatalog.YELLOW_BUTTON_TEXTURE_SLOT, -0.30f, -0.78f);
    }

    private static SimpleWorldState.EntityState createButtonEntity(int entityId,
                                                                  int textureSlot,
                                                                  float x,
                                                                  float y) {
        return new SimpleWorldState.EntityState(entityId,
                0,
                SimpleWorldState.EntityState.ControlMode.AI,
                textureSlot,
                x,
                y,
                0.95f,
                0.0f,
                0.0f,
                0.0f,
                0.0f,
                0.0f,
                BUTTON_PRESS_ANIMATION_SPEED,
                0.18f,
                BlendMode.ALPHA,
                1,
                0,
                ScissorRect.disabled(),
                TextureRegion.full(),
                2.0f,
                2.0f,
                SimpleWorldState.EntityState.TouchInteraction.BUTTON_PRESS);
    }
}
