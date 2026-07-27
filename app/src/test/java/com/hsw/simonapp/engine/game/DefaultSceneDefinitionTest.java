package com.hsw.simonapp.engine.game;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.hsw.simonapp.engine.loop.core.WorldState;
import com.hsw.simonapp.engine.loop.defaults.SimpleWorldState;

import org.junit.Test;

public class DefaultSceneDefinitionTest {
    private static final float FLOAT_DELTA = 0.0001f;

    @Test
    public void createInitialWorldState_placesSimonButtonsInTwoRows() {
        WorldState worldState = new DefaultSceneDefinition().createInitialWorldState();
        SimpleWorldState simpleWorldState = (SimpleWorldState) worldState;

        SimpleWorldState.EntityState redButton = simpleWorldState.findEntityById(3);
        SimpleWorldState.EntityState greenButton = simpleWorldState.findEntityById(4);
        SimpleWorldState.EntityState blueButton = simpleWorldState.findEntityById(5);
        SimpleWorldState.EntityState yellowButton = simpleWorldState.findEntityById(6);

        assertEquals(5, simpleWorldState.getEntities().size());
        assertNotNull(redButton);
        assertNotNull(greenButton);
        assertNotNull(blueButton);
        assertNotNull(yellowButton);
        assertEquals(redButton.getY(), greenButton.getY(), FLOAT_DELTA);
        assertTrue(greenButton.getX() < redButton.getX());
        assertEquals(blueButton.getY(), yellowButton.getY(), FLOAT_DELTA);
        assertTrue(blueButton.getY() > redButton.getY());
        assertTrue(yellowButton.getY() > greenButton.getY());
        assertTrue(yellowButton.getX() < blueButton.getX());
        assertEquals(redButton.getX(), blueButton.getX(), FLOAT_DELTA);
        assertEquals(greenButton.getX(), yellowButton.getX(), FLOAT_DELTA);
        assertEquals(redButton.getScaleX(), greenButton.getScaleX(), FLOAT_DELTA);
        assertEquals(redButton.getScaleY(), greenButton.getScaleY(), FLOAT_DELTA);
        assertEquals(redButton.getScaleX(), blueButton.getScaleX(), FLOAT_DELTA);
        assertEquals(redButton.getScaleY(), blueButton.getScaleY(), FLOAT_DELTA);
        assertEquals(greenButton.getScaleX(), yellowButton.getScaleX(), FLOAT_DELTA);
        assertEquals(greenButton.getScaleY(), yellowButton.getScaleY(), FLOAT_DELTA);
    }
}
