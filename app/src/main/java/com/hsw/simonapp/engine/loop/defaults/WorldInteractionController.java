package com.hsw.simonapp.engine.loop.defaults;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public final class WorldInteractionController {

    private static final int NO_SELECTED_ENTITY = Integer.MIN_VALUE;

    private final AtomicInteger selectedEntityId = new AtomicInteger(NO_SELECTED_ENTITY);
    private final ConcurrentLinkedQueue<Integer> reverseDirectionRequests = new ConcurrentLinkedQueue<>();

    private volatile Consumer<Integer> selectedEntityListener;
    private volatile BiConsumer<Float, Float> worldCoordinateTouchListener;

    public void setSelectedEntityListener(Consumer<Integer> selectedEntityListener) {
        this.selectedEntityListener = selectedEntityListener;
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

    Integer pollReverseDirectionRequest() {
        return reverseDirectionRequests.poll();
    }
}
