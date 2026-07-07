package com.hsw.etalonapp.engine.game;

import android.content.res.AssetManager;

import com.hsw.etalonapp.engine.api.TextureRegion;
import com.hsw.etalonapp.engine.assets.TextureAtlasAssetLoader;
import com.hsw.etalonapp.engine.scene.TextureCatalog;
import com.hsw.etalonapp.engine.scene.model.TextureAtlas;

import java.io.IOException;
import java.util.Objects;

public final class DefaultGameDefinition implements GameDefinition {

    private final GameRuntimeFactory runtimeFactory;

    public DefaultGameDefinition() {
        this(new DefaultGameRuntimeFactory());
    }

    public DefaultGameDefinition(GameRuntimeFactory runtimeFactory) {
        this.runtimeFactory = Objects.requireNonNull(runtimeFactory, "runtimeFactory");
    }

    @Override
    public SceneDefinition createSceneDefinition(AssetManager assetManager) {
        return new DefaultSceneDefinition(loadFireAtlasRegion(
                Objects.requireNonNull(assetManager, "assetManager")));
    }

    @Override
    public GameRuntimeFactory runtimeFactory() {
        return runtimeFactory;
    }

    private static TextureRegion loadFireAtlasRegion(AssetManager assetManager) {
        try {
            TextureAtlas fireAtlas = new TextureAtlasAssetLoader(assetManager)
                    .load(TextureCatalog.FIRE_ATLAS_METADATA_ASSET);
            int demoFrame = Math.min(12, fireAtlas.frameCount() - 1);
            return fireAtlas.getFrame(demoFrame).getTextureRegion();
        } catch (IOException e) {
            return TextureRegion.full();
        }
    }
}
