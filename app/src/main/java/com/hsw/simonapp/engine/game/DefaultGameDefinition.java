package com.hsw.simonapp.engine.game;

import android.content.res.AssetManager;

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
        Objects.requireNonNull(assetManager, "assetManager");
        return new DefaultSceneDefinition();
    }

    @Override
    public GameRuntimeFactory runtimeFactory() {
        return runtimeFactory;
    }
}
