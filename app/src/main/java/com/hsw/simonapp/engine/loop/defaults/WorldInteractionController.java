package com.hsw.simonapp.engine.loop.defaults;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public final class WorldInteractionController {

    private static final int NO_SELECTED_ENTITY = Integer.MIN_VALUE;

    private final AtomicInteger selectedEntityId = new AtomicInteger(NO_SELECTED_ENTITY);
    private final ConcurrentLinkedQueue<Integer> reverseDirectionRequests = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<Integer> buttonPressRequests = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<Boolean> startGameRequests = new ConcurrentLinkedQueue<>();

    private volatile Consumer<Integer> selectedEntityListener;
    private volatile Consumer<Integer> buttonPressListener;
    private volatile Consumer<Integer> scoreListener;
    private volatile Consumer<Integer> gameOverListener;
    private volatile BiConsumer<Float, Float> worldCoordinateTouchListener;

    public void setSelectedEntityListener(Consumer<Integer> selectedEntityListener) {
        this.selectedEntityListener = selectedEntityListener;
    }

    public void setButtonPressListener(Consumer<Integer> buttonPressListener) {
        this.buttonPressListener = buttonPressListener;
    }

    public void setScoreListener(Consumer<Integer> scoreListener) {
        this.scoreListener = scoreListener;
    }

    public void setGameOverListener(Consumer<Integer> gameOverListener) {
        this.gameOverListener = gameOverListener;
    }

    public void setWorldCoordinateTouchListener(BiConsumer<Float, Float> worldCoordinateTouchListener) {
        this.worldCoordinateTouchListener = worldCoordinateTouchListener;
    }

    public Integer getSelectedEntityId() {
        int entityId = selectedEntityId.get();
        if (entityId == NO_SELECTED_ENTITY) {
            return null;
        }
        return entityId;
    }

    public void selectEntity(int entityId) {
        selectedEntityId.set(entityId);
        Consumer<Integer> listener = selectedEntityListener;
        if (listener != null) {
            listener.accept(entityId);
        }
    }

    public void showWorldCoordinates(float x, float y) {
        selectedEntityId.set(NO_SELECTED_ENTITY);
        BiConsumer<Float, Float> listener = worldCoordinateTouchListener;
        if (listener != null) {
            listener.accept(x, y);
        }
    }

    public boolean requestReverseSelectedEntityDirection() {
        Integer entityId = getSelectedEntityId();
        if (entityId == null) {
            return false;
        }
        reverseDirectionRequests.add(entityId);
        return true;
    }

    public void requestButtonPress(int entityId) {
        buttonPressRequests.add(entityId);
        Consumer<Integer> listener = buttonPressListener;
        if (listener != null) {
            listener.accept(entityId);
        }
    }

    public void requestStartGame() {
        startGameRequests.add(Boolean.TRUE);
    }

    public void notifyScore(int score) {
        Consumer<Integer> listener = scoreListener;
        if (listener != null) {
            listener.accept(score);
        }
    }

    public void notifyGameOver(int score) {
        Consumer<Integer> listener = gameOverListener;
        if (listener != null) {
            listener.accept(score);
        }
    }

    Boolean pollStartGameRequest() {
        return startGameRequests.poll();
    }

    Integer pollReverseDirectionRequest() {
        return reverseDirectionRequests.poll();
    }

    Integer pollButtonPressRequest() {
        return buttonPressRequests.poll();
    }
}
