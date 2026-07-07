package com.hsw.simonapp.engine.game;

import com.hsw.simonapp.engine.loop.core.WorldState;
import com.hsw.simonapp.engine.scene.model.ShapeTextureRegistration;
import com.hsw.simonapp.engine.scene.model.TextureSpec;

import java.util.List;

public interface SceneDefinition {

    List<TextureSpec> textureSpecs();

    List<ShapeTextureRegistration> shapeTextureRegistrations();

    WorldState createInitialWorldState();
}
