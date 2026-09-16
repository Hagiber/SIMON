package com.hsw.simonapp.engine.game;

import com.hsw.simonapp.engine.api.OrthoCamera;
import com.hsw.simonapp.engine.input.QueuedInputReader;
import com.hsw.simonapp.engine.loop.core.CollisionDetector;
import com.hsw.simonapp.engine.loop.core.CollisionResolver;
import com.hsw.simonapp.engine.loop.core.DeterministicWorldUpdater;
import com.hsw.simonapp.engine.loop.core.GameLoop;
import com.hsw.simonapp.engine.loop.core.InputReader;
import com.hsw.simonapp.engine.loop.core.WorldState;
import com.hsw.simonapp.engine.loop.core.WorldUpdater;
import com.hsw.simonapp.engine.loop.defaults.SimpleCollisionDetector;
import com.hsw.simonapp.engine.loop.defaults.SimpleCollisionResolver;
import com.hsw.simonapp.engine.loop.defaults.SimonWorldUpdater;
import com.hsw.simonapp.engine.loop.defaults.WorldInteractionController;

import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public final class DefaultGameRuntimeFactory implements GameRuntimeFactory {

    @Override
    public GameRuntime createRuntime(Context context) {
        Context nonNullContext = Objects.requireNonNull(context, "context");
        WorldInteractionController interactionController = new WorldInteractionController();
        SimonWorldUpdater simonWorldUpdater = new SimonWorldUpdater(interactionController,
                nonNullContext.getAudioEventQueue());

        InputReader inputReader = new QueuedInputReader(nonNullContext.getInputEventQueue());
        SceneDefinition sceneDefinition = nonNullContext.getSceneDefinition();
        WorldUpdater worldUpdater = new DeterministicWorldUpdater(simonWorldUpdater,
                simonWorldUpdater,
                simonWorldUpdater);
        CollisionDetector collisionDetector = new SimpleCollisionDetector();
        CollisionResolver collisionResolver = new SimpleCollisionResolver();

        GameLoop gameLoop = new GameLoop(sceneDefinition.createInitialWorldState(),
                inputReader,
                worldUpdater,
                collisionDetector,
                collisionResolver,
                nonNullContext.getAudioEventQueue(),
                nonNullContext.getAudioSubsystem(),
                nonNullContext.getFrameRenderer(),
                nonNullContext.getFrameTimingSink());

        return new GameRuntime(gameLoop,
                new DefaultViewportAdapter(simonWorldUpdater, gameLoop, sceneDefinition),
                new DefaultInteractionAdapter(interactionController));
    }

    private static final class DefaultViewportAdapter implements ViewportAdapter {
        private final SimonWorldUpdater simonWorldUpdater;
        private final GameLoop gameLoop;
        private final SceneDefinition sceneDefinition;

        private DefaultViewportAdapter(SimonWorldUpdater simonWorldUpdater,
                                       GameLoop gameLoop,
                                       SceneDefinition sceneDefinition) {
            this.simonWorldUpdater = Objects.requireNonNull(simonWorldUpdater, "simonWorldUpdater");
            this.gameLoop = Objects.requireNonNull(gameLoop, "gameLoop");
            this.sceneDefinition = Objects.requireNonNull(sceneDefinition, "sceneDefinition");
        }

        @Override
        public void resizeViewport(int width, int height) {
            if (width <= 0 || height <= 0) {
                simonWorldUpdater.resizeViewport(width, height);
                return;
            }
            resizeViewport(width, height, sceneDefinition.cameraForViewport(width, height));
        }

        @Override
        public void resizeViewport(int width, int height, OrthoCamera camera) {
            simonWorldUpdater.resizeViewport(width, height, camera);
            WorldState currentWorldState = gameLoop.getCurrentWorldState();
            WorldState layoutWorldState = sceneDefinition.layoutWorldState(currentWorldState,
                    width,
                    height,
                    camera);
            if (layoutWorldState != currentWorldState) {
                gameLoop.replaceWorldState(layoutWorldState);
            }
        }
    }

    private static final class DefaultInteractionAdapter implements InteractionAdapter {
        private final WorldInteractionController interactionController;

        private DefaultInteractionAdapter(WorldInteractionController interactionController) {
            this.interactionController = Objects.requireNonNull(interactionController,
                    "interactionController");
        }

        @Override
        public void setSelectedEntityListener(Consumer<Integer> selectedEntityListener) {
            interactionController.setSelectedEntityListener(selectedEntityListener);
        }

        @Override
        public void setButtonPressListener(Consumer<Integer> buttonPressListener) {
            interactionController.setButtonPressListener(buttonPressListener);
        }

        @Override
        public void setScoreListener(Consumer<Integer> scoreListener) {
            interactionController.setScoreListener(scoreListener);
        }

        @Override
        public void setGameOverListener(Consumer<Integer> gameOverListener) {
            interactionController.setGameOverListener(gameOverListener);
        }

        @Override
        public void setWorldCoordinateTouchListener(BiConsumer<Float, Float> worldCoordinateTouchListener) {
            interactionController.setWorldCoordinateTouchListener(worldCoordinateTouchListener);
        }

        @Override
        public boolean requestReverseSelectedEntityDirection() {
            return interactionController.requestReverseSelectedEntityDirection();
        }

        @Override
        public void startSimonGame() {
            interactionController.requestStartGame();
        }
    }

}
