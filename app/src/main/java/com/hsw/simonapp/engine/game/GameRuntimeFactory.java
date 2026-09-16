package com.hsw.simonapp.engine.game;

import com.hsw.simonapp.engine.api.OrthoCamera;
import com.hsw.simonapp.engine.audio.AudioEventQueue;
import com.hsw.simonapp.engine.audio.AudioSubsystem;
import com.hsw.simonapp.engine.input.InputEventQueue;
import com.hsw.simonapp.engine.loop.core.FrameRenderer;
import com.hsw.simonapp.engine.loop.core.FrameTimingSink;
import com.hsw.simonapp.engine.loop.core.GameLoop;

import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public interface GameRuntimeFactory {

    GameRuntime createRuntime(Context context);

    final class Context {
        private final InputEventQueue inputEventQueue;
        private final AudioEventQueue audioEventQueue;
        private final AudioSubsystem audioSubsystem;
        private final FrameRenderer frameRenderer;
        private final FrameTimingSink frameTimingSink;
        private final SceneDefinition sceneDefinition;

        public Context(InputEventQueue inputEventQueue,
                       AudioEventQueue audioEventQueue,
                       AudioSubsystem audioSubsystem,
                       FrameRenderer frameRenderer,
                       FrameTimingSink frameTimingSink,
                       SceneDefinition sceneDefinition) {
            this.inputEventQueue = Objects.requireNonNull(inputEventQueue, "inputEventQueue");
            this.audioEventQueue = Objects.requireNonNull(audioEventQueue, "audioEventQueue");
            this.audioSubsystem = Objects.requireNonNull(audioSubsystem, "audioSubsystem");
            this.frameRenderer = Objects.requireNonNull(frameRenderer, "frameRenderer");
            this.frameTimingSink = Objects.requireNonNull(frameTimingSink, "frameTimingSink");
            this.sceneDefinition = Objects.requireNonNull(sceneDefinition, "sceneDefinition");
        }

        public InputEventQueue getInputEventQueue() {
            return inputEventQueue;
        }

        public AudioEventQueue getAudioEventQueue() {
            return audioEventQueue;
        }

        public AudioSubsystem getAudioSubsystem() {
            return audioSubsystem;
        }

        public FrameRenderer getFrameRenderer() {
            return frameRenderer;
        }

        public FrameTimingSink getFrameTimingSink() {
            return frameTimingSink;
        }

        public SceneDefinition getSceneDefinition() {
            return sceneDefinition;
        }
    }

    final class GameRuntime {
        private final GameLoop gameLoop;
        private final ViewportAdapter viewportAdapter;
        private final InteractionAdapter interactionAdapter;

        public GameRuntime(GameLoop gameLoop,
                           ViewportAdapter viewportAdapter,
                           InteractionAdapter interactionAdapter) {
            this.gameLoop = Objects.requireNonNull(gameLoop, "gameLoop");
            this.viewportAdapter = Objects.requireNonNull(viewportAdapter, "viewportAdapter");
            this.interactionAdapter = Objects.requireNonNull(interactionAdapter, "interactionAdapter");
        }

        public GameLoop getGameLoop() {
            return gameLoop;
        }

        public ViewportAdapter getViewportAdapter() {
            return viewportAdapter;
        }

        public InteractionAdapter getInteractionAdapter() {
            return interactionAdapter;
        }
    }

    interface ViewportAdapter {
        void resizeViewport(int width, int height);

        default void resizeViewport(int width, int height, OrthoCamera camera) {
            resizeViewport(width, height);
        }

        static ViewportAdapter ignoring() {
            return (width, height) -> {
            };
        }
    }

    interface InteractionAdapter {
        void setSelectedEntityListener(Consumer<Integer> selectedEntityListener);

        default void setButtonPressListener(Consumer<Integer> buttonPressListener) {
        }

        default void setScoreListener(Consumer<Integer> scoreListener) {
        }

        default void setGameOverListener(Consumer<Integer> gameOverListener) {
        }

        void setWorldCoordinateTouchListener(BiConsumer<Float, Float> worldCoordinateTouchListener);

        boolean requestReverseSelectedEntityDirection();

        default void startSimonGame() {
        }

        static InteractionAdapter ignoring() {
            return new InteractionAdapter() {
                @Override
                public void setSelectedEntityListener(Consumer<Integer> selectedEntityListener) {
                }

                @Override
                public void setWorldCoordinateTouchListener(BiConsumer<Float, Float> worldCoordinateTouchListener) {
                }

                @Override
                public boolean requestReverseSelectedEntityDirection() {
                    return false;
                }
            };
        }
    }

}
