package com.hsw.simonapp.engine.loop.defaults;

import com.hsw.simonapp.engine.api.TouchInputEvent;
import com.hsw.simonapp.engine.input.TouchInputSnapshot;
import com.hsw.simonapp.engine.loop.core.InputSnapshot;

final class TouchInputStateReducer {

    private final WorldBounds worldBounds;

    private Integer activeTouchPointerId;
    private Float activeTouchTargetX;
    private Float activeTouchTargetY;

    TouchInputStateReducer() {
        this(WorldBounds.defaults());
    }

    TouchInputStateReducer(WorldBounds worldBounds) {
        this.worldBounds = worldBounds;
    }

    void reduce(InputSnapshot inputSnapshot) {
        if (!(inputSnapshot instanceof TouchInputSnapshot)) {
            return;
        }

        TouchInputSnapshot touchInputSnapshot = (TouchInputSnapshot) inputSnapshot;
        for (TouchInputEvent event : touchInputSnapshot.getEvents()) {
            switch (event.getAction()) {
                case DOWN:
                    break;
                case MOVE:
                    if (activeTouchPointerId != null && activeTouchPointerId == event.getPointerId()) {
                        updateTouchTarget(event);
                    }
                    break;
                case UP:
                case CANCEL:
                    if (activeTouchPointerId != null && activeTouchPointerId == event.getPointerId()) {
                        clearTouchTarget();
                    }
                    break;
                default:
                    break;
            }
        }
    }

    void startTouchTarget(TouchInputEvent event) {
        activeTouchPointerId = event.getPointerId();
        updateTouchTarget(event);
    }

    void clearTouchTarget() {
        activeTouchPointerId = null;
        activeTouchTargetX = null;
        activeTouchTargetY = null;
    }

    Float getActiveTouchTargetX() {
        return activeTouchTargetX;
    }

    Float getActiveTouchTargetY() {
        return activeTouchTargetY;
    }

    private void updateTouchTarget(TouchInputEvent event) {
        activeTouchTargetX = toWorldX(event);
        activeTouchTargetY = toWorldY(event);
    }

    float toWorldX(TouchInputEvent event) {
        updateViewportSize(event);
        if (event.getSurfaceWidth() <= 0) {
            return 0.0f;
        }
        return worldBounds.toWorldX(event.getXPixels() / event.getSurfaceWidth() * 2.0f - 1.0f);
    }

    float toWorldY(TouchInputEvent event) {
        updateViewportSize(event);
        if (event.getSurfaceHeight() <= 0) {
            return 0.0f;
        }
        return worldBounds.toWorldY(1.0f - event.getYPixels() / event.getSurfaceHeight() * 2.0f);
    }

    private void updateViewportSize(TouchInputEvent event) {
        worldBounds.setViewportSize(event.getSurfaceWidth(), event.getSurfaceHeight());
    }
}
