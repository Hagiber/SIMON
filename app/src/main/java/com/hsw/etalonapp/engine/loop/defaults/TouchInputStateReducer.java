package com.hsw.etalonapp.engine.loop.defaults;

import com.hsw.etalonapp.engine.api.TouchInputEvent;
import com.hsw.etalonapp.engine.input.TouchInputSnapshot;
import com.hsw.etalonapp.engine.loop.core.InputSnapshot;

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
                    if (activeTouchPointerId == null) {
                        activeTouchPointerId = event.getPointerId();
                    }
                    if (activeTouchPointerId == event.getPointerId()) {
                        updateTouchTarget(event);
                    }
                    break;
                case MOVE:
                    if (activeTouchPointerId != null && activeTouchPointerId == event.getPointerId()) {
                        updateTouchTarget(event);
                    }
                    break;
                case UP:
                case CANCEL:
                    if (activeTouchPointerId != null && activeTouchPointerId == event.getPointerId()) {
                        activeTouchPointerId = null;
                        activeTouchTargetX = null;
                        activeTouchTargetY = null;
                    }
                    break;
                default:
                    break;
            }
        }
    }

    Float getActiveTouchTargetX() {
        return activeTouchTargetX;
    }

    Float getActiveTouchTargetY() {
        return activeTouchTargetY;
    }

    private void updateTouchTarget(TouchInputEvent event) {
        worldBounds.setViewportSize(event.getSurfaceWidth(), event.getSurfaceHeight());
        activeTouchTargetX = toWorldX(event);
        activeTouchTargetY = toWorldY(event);
    }

    float toWorldX(TouchInputEvent event) {
        if (event.getSurfaceWidth() <= 0) {
            return 0.0f;
        }
        return worldBounds.toWorldX(event.getXPixels() / event.getSurfaceWidth() * 2.0f - 1.0f);
    }

    float toWorldY(TouchInputEvent event) {
        if (event.getSurfaceHeight() <= 0) {
            return 0.0f;
        }
        return worldBounds.toWorldY(1.0f - event.getYPixels() / event.getSurfaceHeight() * 2.0f);
    }
}
