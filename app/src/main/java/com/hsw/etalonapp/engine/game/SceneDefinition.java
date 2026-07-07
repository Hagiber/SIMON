package com.hsw.etalonapp.engine.game;

import com.hsw.etalonapp.engine.loop.core.WorldState;
import com.hsw.etalonapp.engine.scene.model.ShapeTextureRegistration;
import com.hsw.etalonapp.engine.scene.model.TextureSpec;

import java.util.List;

public interface SceneDefinition {

    List<TextureSpec> textureSpecs();

    List<ShapeTextureRegistration> shapeTextureRegistrations();

    WorldState createInitialWorldState();
}
