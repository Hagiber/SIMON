package com.hsw.simonapp.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class GameSessionStateTest {

    @Test
    public void startRun_resetsCurrentResultAndAcceptsScoreUpdates() {
        GameSessionState state = new GameSessionState(true, 4, 6);

        state.startRun();

        assertTrue(state.isRunning());
        assertEquals(0, state.getCurrentResult());

        assertTrue(state.setCurrentResult(2));
        assertEquals(2, state.getCurrentResult());
    }

    @Test
    public void setCurrentResult_ignoresScoreWhenNotRunning() {
        GameSessionState state = new GameSessionState(false, 0, 0);

        assertFalse(state.setCurrentResult(1));

        assertEquals(0, state.getCurrentResult());
        assertFalse(state.hasLastResult());
    }

    @Test
    public void finishRun_storesLastResultAndUpdatesRecord() {
        GameSessionState state = new GameSessionState(true, 2, 3);
        state.startRun();
        state.setCurrentResult(4);

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

    @Test
    public void constructor_restoresRunningRunWithoutPromotingCurrentResultToRecord() {
        GameSessionState state = new GameSessionState(true, 8, true, 5, 6);

        assertTrue(state.isRunning());
        assertEquals(8, state.getCurrentResult());
        assertTrue(state.hasLastResult());
        assertEquals(5, state.getLastResult());
        assertEquals(6, state.getRecord());
    }

    @Test
    public void constructor_clearsCurrentResultWhenRestoredStateIsNotRunning() {
        GameSessionState state = new GameSessionState(false, 9, false, -2, -3);

        assertFalse(state.isRunning());
        assertEquals(0, state.getCurrentResult());
        assertFalse(state.hasLastResult());
        assertEquals(0, state.getLastResult());
        assertEquals(0, state.getRecord());
    }

    @Test
    public void finishRun_afterRestoredRunningRunStoresCurrentResult() {
        GameSessionState state = new GameSessionState(true, 5, true, 3, 4);

        assertTrue(state.finishRun());

        assertFalse(state.isRunning());
        assertTrue(state.hasLastResult());
        assertEquals(5, state.getLastResult());
        assertEquals(5, state.getRecord());
    }

    @Test
    public void finishRun_withExplicitScoreStoresFinalResult() {
        GameSessionState state = new GameSessionState(true, 2, true, 4, 6);

        assertTrue(state.finishRun(7));

        assertFalse(state.isRunning());
        assertEquals(7, state.getLastResult());
        assertEquals(7, state.getRecord());
    }
}
