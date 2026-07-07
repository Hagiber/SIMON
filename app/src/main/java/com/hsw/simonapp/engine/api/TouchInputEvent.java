package com.hsw.simonapp.engine.api;

import java.util.Objects;

public final class TouchInputEvent {

    public enum Action {
        DOWN,
        MOVE,
        UP,
        CANCEL
    }

    private final Action action;
    private final int pointerId;
    private final float xPixels;
    private final float yPixels;
    private final int surfaceWidth;
    private final int surfaceHeight;
    private final long eventTimeMillis;

    public TouchInputEvent(Action action,
                           int pointerId,
                           float xPixels,
                           float yPixels,
                           int surfaceWidth,
                           int surfaceHeight,
                           long eventTimeMillis) {
        this.action = Objects.requireNonNull(action, "action");
        this.pointerId = pointerId;
        this.xPixels = xPixels;
        this.yPixels = yPixels;
        this.surfaceWidth = surfaceWidth;
        this.surfaceHeight = surfaceHeight;
        this.eventTimeMillis = eventTimeMillis;
    }

    public Action getAction() {
        return action;
    }

    public int getPointerId() {
        return pointerId;
    }

    public float getXPixels() {
        return xPixels;
    }

    public float getYPixels() {
        return yPixels;
    }

    public int getSurfaceWidth() {
        return surfaceWidth;
    }

    public int getSurfaceHeight() {
        return surfaceHeight;
    }

    public long getEventTimeMillis() {
        return eventTimeMillis;
    }
}
