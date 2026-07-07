package com.hsw.etalonapp.engine.loop.core;

import java.util.Objects;

public final class DeterministicWorldUpdater implements WorldUpdater {

    private final InputApplier inputApplier;
    private final GameplayDecider gameplayDecider;
    private final PhysicsIntegrator physicsIntegrator;

    public DeterministicWorldUpdater(InputApplier inputApplier,
                                     GameplayDecider gameplayDecider,
                                     PhysicsIntegrator physicsIntegrator) {
        this.inputApplier = Objects.requireNonNull(inputApplier, "inputApplier");
        this.gameplayDecider = Objects.requireNonNull(gameplayDecider, "gameplayDecider");
        this.physicsIntegrator = Objects.requireNonNull(physicsIntegrator, "physicsIntegrator");
    }

    @Override
    public WorldState update(FrameContext frameContext, WorldState currentWorldState, InputSnapshot inputSnapshot) {
        WorldState inputAppliedState = Objects.requireNonNull(
                inputApplier.applyInput(frameContext, currentWorldState, inputSnapshot),
                "inputAppliedState");
        WorldState gameplayDecidedState = Objects.requireNonNull(
                gameplayDecider.decideGameplay(frameContext, inputAppliedState),
                "gameplayDecidedState");
        return Objects.requireNonNull(
                physicsIntegrator.integratePhysics(frameContext, gameplayDecidedState),
                "physicsIntegratedState");
    }
}


