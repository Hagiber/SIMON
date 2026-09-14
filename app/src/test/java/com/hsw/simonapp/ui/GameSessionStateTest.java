package com.hsw.simonapp.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class GameSessionStateTest {

    @Test
    public void startRun_resetsCurrentResultAndStartsCounting() {
        GameSessionState state = new GameSessionState(true, 4, 6);

        state.startRun();

        assertTrue(state.isRunning());
        assertEquals(0, state.getCurrentResult());

        assertTrue(state.recordButtonPress());
        assertEquals(1, state.getCurrentResult());
    }

    @Test
    public void recordButtonPress_ignoresPressesWhenNotRunning() {
        GameSessionState state = new GameSessionState(false, 0, 0);

        assertFalse(state.recordButtonPress());

        assertEquals(0, state.getCurrentResult());
        assertFalse(state.hasLastResult());
    }

    @Test
    public void finishRun_storesLastResultAndUpdatesRecord() {
        GameSessionState state = new GameSessionState(true, 2, 3);
        state.startRun();
        state.recordButtonPress();
        state.recordButtonPress();
        state.recordButtonPress();
        state.recordButtonPress();

        assertTrue(state.finishRun());

        assertFalse(state.isRunning());
        assertTrue(state.hasLastResult());
        assertEquals(4, state.getLastResult());
        assertEquals(4, state.getRecord());
    }

    @Test
    public void constructor_keepsRecordAtLeastLastResult() {
        GameSessionState state = new GameSessionState(true, 7, 3);

        assertEquals(7, state.getLastResult());
        assertEquals(7, state.getRecord());
    }
}
