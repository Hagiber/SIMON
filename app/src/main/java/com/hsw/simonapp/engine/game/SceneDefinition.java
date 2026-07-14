package com.hsw.simonapp.engine.game;

import com.hsw.simonapp.engine.api.OrthoCamera;
import com.hsw.simonapp.engine.loop.core.WorldState;
import com.hsw.simonapp.engine.scene.model.ShapeTextureRegistration;
import com.hsw.simonapp.engine.scene.model.TextureSpec;

import java.util.List;

public interface SceneDefinition {

    List<TextureSpec> textureSpecs();

    List<ShapeTextureRegistration> shapeTextureRegistrations();

    WorldState createInitialWorldState();

    default OrthoCamera cameraForViewport(int viewportWidth, int viewportHeight) {
        return OrthoCamera.defaults();
    }

    default WorldState layoutWorldState(WorldState worldState, int viewportWidth, int viewportHeight) {
        return worldState;
    }

    default WorldState layoutWorldState(WorldState worldState,
                                        int viewportWidth,
                                        int viewportHeight,
                                        OrthoCamera camera) {
        return layoutWorldState(worldState, viewportWidth, viewportHeight);
    }
}
