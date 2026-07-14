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

    private final List<TextureSpec> textureSpecs;
    private final List<ShapeTextureRegistration> shapeTextureRegistrations;
    private final CameraLayoutPolicy cameraLayoutPolicy;
    private final SceneLayout sceneLayout;

    DefaultSceneDefinition() {
        this.textureSpecs = Collections.unmodifiableList(TextureCatalog.defaultScene());
        this.shapeTextureRegistrations = Collections.unmodifiableList(TextureCatalog.defaultShapeTextures());
        this.cameraLayoutPolicy = new CameraLayoutPolicy(2.0f, 2.0f, FitMode.FIT_SHORT_SIDE);
        this.sceneLayout = new SceneLayout(TextureCatalog::textureDimensionsForSlot,
                Collections.singletonList(new SpriteLayoutRule(TextureCatalog.SIMONFRAME_TEXTURE_SLOT,
                        FitMode.FIT_WIDTH,
                        0.96f)));
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
                new SimpleWorldState.EntityState(1, 0, SimpleWorldState.EntityState.ControlMode.HUMAN_TOUCH, TextureCatalog.SIMONFRAME_TEXTURE_SLOT, 0.0f, -3.0f, 1.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.15f),

                createShapeEntity(2,
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
                        1)
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
