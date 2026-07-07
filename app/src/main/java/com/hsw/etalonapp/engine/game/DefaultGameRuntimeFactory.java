package com.hsw.etalonapp.engine.game;

import com.hsw.etalonapp.engine.input.QueuedInputReader;
import com.hsw.etalonapp.engine.loop.core.CollisionDetector;
import com.hsw.etalonapp.engine.loop.core.CollisionResolver;
import com.hsw.etalonapp.engine.loop.core.DeterministicWorldUpdater;
import com.hsw.etalonapp.engine.loop.core.GameLoop;
import com.hsw.etalonapp.engine.loop.core.InputReader;
import com.hsw.etalonapp.engine.loop.core.WorldState;
import com.hsw.etalonapp.engine.loop.core.WorldUpdater;
import com.hsw.etalonapp.engine.loop.defaults.SimpleCollisionDetector;
import com.hsw.etalonapp.engine.loop.defaults.SimpleCollisionResolver;
import com.hsw.etalonapp.engine.loop.defaults.SimpleWorldState;
import com.hsw.etalonapp.engine.loop.defaults.SimpleWorldUpdater;
import com.hsw.etalonapp.engine.loop.defaults.WorldInteractionController;
import com.hsw.etalonapp.engine.save.JsonSimpleWorldStateSaveRepository;

import java.io.File;
import java.io.IOException;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public final class DefaultGameRuntimeFactory implements GameRuntimeFactory {

    @Override
    public GameRuntime createRuntime(Context context) {
        Context nonNullContext = Objects.requireNonNull(context, "context");
        WorldInteractionController interactionController = new WorldInteractionController();
        SimpleWorldUpdater simpleWorldUpdater = new SimpleWorldUpdater(interactionController);

        InputReader inputReader = new QueuedInputReader(nonNullContext.getInputEventQueue());
        WorldUpdater worldUpdater = new DeterministicWorldUpdater(simpleWorldUpdater,
                simpleWorldUpdater,
                simpleWorldUpdater);
        CollisionDetector collisionDetector = new SimpleCollisionDetector();
        CollisionResolver collisionResolver = new SimpleCollisionResolver();

        GameLoop gameLoop = new GameLoop(nonNullContext.getSceneDefinition().createInitialWorldState(),
                inputReader,
                worldUpdater,
                collisionDetector,
                collisionResolver,
                nonNullContext.getAudioEventQueue(),
                nonNullContext.getAudioSubsystem(),
                nonNullContext.getFrameRenderer(),
                nonNullContext.getFrameTimingSink());

        return new GameRuntime(gameLoop,
                simpleWorldUpdater::resizeViewport,
                new DefaultInteractionAdapter(interactionController),
                new SimpleWorldStatePersistence());
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
        public void setWorldCoordinateTouchListener(BiConsumer<Float, Float> worldCoordinateTouchListener) {
            interactionController.setWorldCoordinateTouchListener(worldCoordinateTouchListener);
        }

        @Override
        public boolean requestReverseSelectedEntityDirection() {
            return interactionController.requestReverseSelectedEntityDirection();
        }
    }

    private static final class SimpleWorldStatePersistence implements StatePersistence {
        private final JsonSimpleWorldStateSaveRepository saveRepository =
                new JsonSimpleWorldStateSaveRepository();

        @Override
        public void save(WorldState worldState, File saveFile) throws IOException {
            Objects.requireNonNull(saveFile, "saveFile");
            if (!(worldState instanceof SimpleWorldState)) {
                throw new IOException("Unsupported world state for default save: "
                        + (worldState == null ? "null" : worldState.getClass().getName()));
            }
            saveRepository.save(saveFile, ((SimpleWorldState) worldState).copy());
        }

        @Override
        public WorldState load(File saveFile) throws IOException {
            return saveRepository.load(saveFile);
        }
    }
}
