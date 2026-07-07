package com.hsw.simonapp.engine.game;

import com.hsw.simonapp.engine.audio.AudioEventQueue;
import com.hsw.simonapp.engine.audio.AudioSubsystem;
import com.hsw.simonapp.engine.input.InputEventQueue;
import com.hsw.simonapp.engine.loop.core.FrameRenderer;
import com.hsw.simonapp.engine.loop.core.FrameTimingSink;
import com.hsw.simonapp.engine.loop.core.GameLoop;
import com.hsw.simonapp.engine.loop.core.WorldState;

import java.io.File;
import java.io.IOException;
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
        private final StatePersistence statePersistence;

        public GameRuntime(GameLoop gameLoop,
                           ViewportAdapter viewportAdapter,
                           InteractionAdapter interactionAdapter) {
            this(gameLoop,
                    viewportAdapter,
                    interactionAdapter,
                    StatePersistence.unsupported());
        }

        public GameRuntime(GameLoop gameLoop,
                           ViewportAdapter viewportAdapter,
                           InteractionAdapter interactionAdapter,
                           StatePersistence statePersistence) {
            this.gameLoop = Objects.requireNonNull(gameLoop, "gameLoop");
            this.viewportAdapter = Objects.requireNonNull(viewportAdapter, "viewportAdapter");
            this.interactionAdapter = Objects.requireNonNull(interactionAdapter, "interactionAdapter");
            this.statePersistence = Objects.requireNonNull(statePersistence, "statePersistence");
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

        public StatePersistence getStatePersistence() {
            return statePersistence;
        }
    }

    interface ViewportAdapter {
        void resizeViewport(int width, int height);

        static ViewportAdapter ignoring() {
            return (width, height) -> {
            };
        }
    }

    interface InteractionAdapter {
        void setSelectedEntityListener(Consumer<Integer> selectedEntityListener);

        void setWorldCoordinateTouchListener(BiConsumer<Float, Float> worldCoordinateTouchListener);

        boolean requestReverseSelectedEntityDirection();

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

    interface StatePersistence {
        void save(WorldState worldState, File saveFile) throws IOException;

        WorldState load(File saveFile) throws IOException;

        static StatePersistence unsupported() {
            return new StatePersistence() {
                @Override
                public void save(WorldState worldState, File saveFile) throws IOException {
                    throw new IOException("Game runtime does not support save");
                }

                @Override
                public WorldState load(File saveFile) throws IOException {
                    throw new IOException("Game runtime does not support load");
                }
            };
        }
    }
}
