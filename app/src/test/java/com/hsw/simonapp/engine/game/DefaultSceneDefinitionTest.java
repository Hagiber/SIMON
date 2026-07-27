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
    public void createInitialWorldState_placesGreenButtonNextToRedButton() {
        WorldState worldState = new DefaultSceneDefinition().createInitialWorldState();
        SimpleWorldState simpleWorldState = (SimpleWorldState) worldState;

        SimpleWorldState.EntityState redButton = simpleWorldState.findEntityById(3);
        SimpleWorldState.EntityState greenButton = simpleWorldState.findEntityById(4);

        assertEquals(3, simpleWorldState.getEntities().size());
        assertNotNull(redButton);
        assertNotNull(greenButton);
        assertEquals(redButton.getY(), greenButton.getY(), FLOAT_DELTA);
        assertTrue(greenButton.getX() > redButton.getX());
        assertEquals(redButton.getScaleX(), greenButton.getScaleX(), FLOAT_DELTA);
        assertEquals(redButton.getScaleY(), greenButton.getScaleY(), FLOAT_DELTA);
    }
}
