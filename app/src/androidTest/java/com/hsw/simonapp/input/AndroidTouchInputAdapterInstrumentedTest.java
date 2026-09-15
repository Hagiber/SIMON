package com.hsw.simonapp.input;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.view.MotionEvent;
import android.view.View;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.hsw.simonapp.engine.api.TouchInputEvent;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;

@RunWith(AndroidJUnit4.class)
public class AndroidTouchInputAdapterInstrumentedTest {

    @Test
    public void onTouch_actionUpDispatchesInputAndPerformsClick() {
        List<TouchInputEvent> touchInputEvents = new ArrayList<>();
        AndroidTouchInputAdapter adapter = new AndroidTouchInputAdapter(touchInputEvents::add);
        ClickTrackingView view = new ClickTrackingView(
                InstrumentationRegistry.getInstrumentation().getTargetContext());
        view.layout(0, 0, 320, 240);

        MotionEvent motionEvent = MotionEvent.obtain(100L, 150L,
                MotionEvent.ACTION_UP, 12f, 34f, 0);
        try {
            assertTrue(adapter.onTouch(view, motionEvent));
        } finally {
            motionEvent.recycle();
        }

        assertEquals(1, view.getPerformClickCount());
        assertEquals(1, touchInputEvents.size());
        TouchInputEvent touchInputEvent = touchInputEvents.get(0);
        assertEquals(TouchInputEvent.Action.UP, touchInputEvent.getAction());
        assertEquals(12f, touchInputEvent.getXPixels(), 0.001f);
        assertEquals(34f, touchInputEvent.getYPixels(), 0.001f);
        assertEquals(320, touchInputEvent.getSurfaceWidth());
        assertEquals(240, touchInputEvent.getSurfaceHeight());
        assertEquals(150L, touchInputEvent.getEventTimeMillis());
    }

    private static final class ClickTrackingView extends View {

        private int performClickCount;

        ClickTrackingView(Context context) {
            super(context);
        }

        @Override
        public boolean performClick() {
            performClickCount++;
            return super.performClick();
        }

        int getPerformClickCount() {
            return performClickCount;
        }
    }
}
