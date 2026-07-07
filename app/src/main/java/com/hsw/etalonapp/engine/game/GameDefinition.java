package com.hsw.etalonapp.engine.game;

import android.content.res.AssetManager;

public interface GameDefinition {

    SceneDefinition createSceneDefinition(AssetManager assetManager);

    GameRuntimeFactory runtimeFactory();
}
