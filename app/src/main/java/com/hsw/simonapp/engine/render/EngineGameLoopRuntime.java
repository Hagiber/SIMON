package com.hsw.simonapp.engine.render;

import android.content.res.AssetManager;

import com.hsw.simonapp.engine.api.OrthoCamera;
import com.hsw.simonapp.engine.api.TouchInputEvent;
import com.hsw.simonapp.engine.audio.AssetBackedBounceAudioPlayer;
import com.hsw.simonapp.engine.audio.AudioEventQueue;
import com.hsw.simonapp.engine.audio.AudioSubsystem;
import com.hsw.simonapp.engine.audio.EventDrivenAudioSubsystem;
import com.hsw.simonapp.engine.game.DefaultGameDefinition;
import com.hsw.simonapp.engine.game.GameDefinition;
import com.hsw.simonapp.engine.game.GameRuntimeFactory;
import com.hsw.simonapp.engine.game.SceneDefinition;
import com.hsw.simonapp.engine.input.InputEventQueue;
import com.hsw.simonapp.engine.loop.core.FrameContext;
import com.hsw.simonapp.engine.loop.core.FrameRenderer;
import com.hsw.simonapp.engine.loop.core.FrameTimingSink;
import com.hsw.simonapp.engine.loop.core.GameLoop;
import com.hsw.simonapp.engine.loop.core.WorldState;

import java.io.File;
import java.io.IOException;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

final class EngineGameLoopRuntime {

    private final InputEventQueue inputEventQueue;
    private final GameLoop gameLoop;
    private final FrameRenderer frameRenderer;
    private final SceneDefinition sceneDefinition;
    private final GameRuntimeFactory.ViewportAdapter viewportAdapter;
    private final GameRuntimeFactory.InteractionAdapter interactionAdapter;
    private final GameRuntimeFactory.StatePersistence statePersistence;

    EngineGameLoopRuntime(AssetManager assetManager,
                          FrameRenderer frameRenderer,
                          FrameTimingSink frameTimingSink) {
        this(assetManager,
                frameRenderer,
                frameTimingSink,
                new DefaultGameDefinition());
    }

    EngineGameLoopRuntime(AssetManager assetManager,
                          FrameRenderer frameRenderer,
                          FrameTimingSink frameTimingSink,
                          GameDefinition gameDefinition) {
        AssetManager nonNullAssetManager = Objects.requireNonNull(assetManager, "assetManager");
        GameDefinition nonNullGameDefinition = Objects.requireNonNull(gameDefinition, "gameDefinition");
        this.inputEventQueue = new InputEventQueue();
        SceneDefinition sceneDefinition = Objects.requireNonNull(
                nonNullGameDefinition.createSceneDefinition(nonNullAssetManager),
                "sceneDefinition");
        GameRuntimeFactory.GameRuntime runtime = createRuntime(nonNullAssetManager,
                Objects.requireNonNull(frameRenderer, "frameRenderer"),
                Objects.requireNonNull(frameTimingSink, "frameTimingSink"),
                nonNullGameDefinition,
                sceneDefinition,
                inputEventQueue);
        this.gameLoop = runtime.getGameLoop();
        this.frameRenderer = Objects.requireNonNull(frameRenderer, "frameRenderer");
        this.sceneDefinition = sceneDefinition;
        this.viewportAdapter = runtime.getViewportAdapter();
        this.interactionAdapter = runtime.getInteractionAdapter();
        this.statePersistence = runtime.getStatePersistence();
    }

    EngineGameLoopRuntime(AssetManager assetManager,
                          FrameRenderer frameRenderer,
                          FrameTimingSink frameTimingSink,
                          GameDefinition gameDefinition,
                          SceneDefinition sceneDefinition) {
        AssetManager nonNullAssetManager = Objects.requireNonNull(assetManager, "assetManager");
        FrameRenderer nonNullFrameRenderer = Objects.requireNonNull(frameRenderer, "frameRenderer");
        FrameTimingSink nonNullFrameTimingSink = Objects.requireNonNull(frameTimingSink, "frameTimingSink");
        GameDefinition nonNullGameDefinition = Objects.requireNonNull(gameDefinition, "gameDefinition");
        SceneDefinition nonNullSceneDefinition = Objects.requireNonNull(sceneDefinition, "sceneDefinition");

        InputEventQueue inputEventQueue = new InputEventQueue();
        this.inputEventQueue = inputEventQueue;
        this.frameRenderer = nonNullFrameRenderer;
        this.sceneDefinition = nonNullSceneDefinition;
        GameRuntimeFactory.GameRuntime runtime = createRuntime(nonNullAssetManager,
                nonNullFrameRenderer,
                nonNullFrameTimingSink,
                nonNullGameDefinition,
                nonNullSceneDefinition,
                inputEventQueue);
        this.gameLoop = runtime.getGameLoop();
        this.viewportAdapter = runtime.getViewportAdapter();
        this.interactionAdapter = runtime.getInteractionAdapter();
        this.statePersistence = runtime.getStatePersistence();
    }

    void queueTouchInput(TouchInputEvent touchInputEvent) {
        inputEventQueue.push(touchInputEvent);
    }

    void setSelectedEntityListener(Consumer<Integer> selectedEntityListener) {
        interactionAdapter.setSelectedEntityListener(selectedEntityListener);
    }

    void setWorldCoordinateTouchListener(BiConsumer<Float, Float> worldCoordinateTouchListener) {
        interactionAdapter.setWorldCoordinateTouchListener(worldCoordinateTouchListener);
    }

    boolean requestReverseSelectedEntityDirection() {
        return interactionAdapter.requestReverseSelectedEntityDirection();
    }

    synchronized void runFrame(FrameContext frameContext) {
        gameLoop.runFrame(frameContext);
    }

    synchronized void resizeViewport(int width, int height) {
        if (width <= 0 || height <= 0) {
            viewportAdapter.resizeViewport(width, height);
            return;
        }
        OrthoCamera camera = sceneDefinition.cameraForViewport(width, height);
        if (frameRenderer instanceof ViewportAwareFrameRenderer) {
            ((ViewportAwareFrameRenderer) frameRenderer).setCamera(camera);
        }
        viewportAdapter.resizeViewport(width, height, camera);
    }

    synchronized boolean saveWorld(File saveFile) {
        try {
            statePersistence.save(gameLoop.getCurrentWorldState(),
                    Objects.requireNonNull(saveFile, "saveFile"));
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    synchronized boolean loadWorld(File saveFile) {
        try {
            WorldState loadedWorldState = statePersistence.load(Objects.requireNonNull(saveFile, "saveFile"));
            if (loadedWorldState == null) {
                return false;
            }
            gameLoop.replaceWorldState(loadedWorldState);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private static GameRuntimeFactory.GameRuntime createRuntime(AssetManager assetManager,
                                                               FrameRenderer frameRenderer,
                                                               FrameTimingSink frameTimingSink,
                                                               GameDefinition gameDefinition,
                                                               SceneDefinition sceneDefinition,
                                                               InputEventQueue inputEventQueue) {
        AudioEventQueue audioEventQueue = new AudioEventQueue();
        AudioSubsystem audioSubsystem = new EventDrivenAudioSubsystem(
                audioEventQueue,
                new AssetBackedBounceAudioPlayer(assetManager));
        GameRuntimeFactory.Context runtimeContext = new GameRuntimeFactory.Context(inputEventQueue,
                audioEventQueue,
                audioSubsystem,
                frameRenderer,
                frameTimingSink,
                sceneDefinition);
        GameRuntimeFactory runtimeFactory = Objects.requireNonNull(gameDefinition.runtimeFactory(),
                "runtimeFactory");
        return Objects.requireNonNull(runtimeFactory.createRuntime(runtimeContext), "gameRuntime");
    }
}
