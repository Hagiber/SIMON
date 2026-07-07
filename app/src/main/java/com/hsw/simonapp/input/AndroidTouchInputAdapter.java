package com.hsw.simonapp.input;

import android.view.MotionEvent;
import android.view.View;

import com.hsw.simonapp.engine.api.TouchInputEvent;

import java.util.Objects;
import java.util.function.Consumer;

public final class AndroidTouchInputAdapter implements View.OnTouchListener {

    private final Consumer<TouchInputEvent> touchInputSink;

    public AndroidTouchInputAdapter(Consumer<TouchInputEvent> touchInputSink) {
        this.touchInputSink = Objects.requireNonNull(touchInputSink, "touchInputSink");
    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        int surfaceWidth = view.getWidth();
        int surfaceHeight = view.getHeight();
        long eventTimeMillis = motionEvent.getEventTime();

        switch (motionEvent.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN:
                pushPointerEvent(motionEvent,
                        motionEvent.getActionIndex(),
                        TouchInputEvent.Action.DOWN,
                        surfaceWidth,
                        surfaceHeight,
                        eventTimeMillis);
                return true;
            case MotionEvent.ACTION_MOVE:
                for (int pointerIndex = 0; pointerIndex < motionEvent.getPointerCount(); pointerIndex++) {
                    pushPointerEvent(motionEvent,
                            pointerIndex,
                            TouchInputEvent.Action.MOVE,
                            surfaceWidth,
                            surfaceHeight,
                            eventTimeMillis);
                }
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
                pushPointerEvent(motionEvent,
                        motionEvent.getActionIndex(),
                        TouchInputEvent.Action.UP,
                        surfaceWidth,
                        surfaceHeight,
                        eventTimeMillis);
                return true;
            case MotionEvent.ACTION_CANCEL:
                for (int pointerIndex = 0; pointerIndex < motionEvent.getPointerCount(); pointerIndex++) {
                    pushPointerEvent(motionEvent,
                            pointerIndex,
                            TouchInputEvent.Action.CANCEL,
                            surfaceWidth,
                            surfaceHeight,
                            eventTimeMillis);
                }
                return true;
            default:
                return false;
        }
    }

    private void pushPointerEvent(MotionEvent motionEvent,
                                  int pointerIndex,
                                  TouchInputEvent.Action action,
                                  int surfaceWidth,
                                  int surfaceHeight,
                                  long eventTimeMillis) {
        touchInputSink.accept(new TouchInputEvent(action,
                motionEvent.getPointerId(pointerIndex),
                motionEvent.getX(pointerIndex),
                motionEvent.getY(pointerIndex),
                surfaceWidth,
                surfaceHeight,
                eventTimeMillis));
    }
}
