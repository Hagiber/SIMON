package com.hsw.etalonapp.engine.save;

import static org.junit.Assert.assertEquals;

import com.hsw.etalonapp.engine.api.BlendMode;
import com.hsw.etalonapp.engine.api.ScissorRect;
import com.hsw.etalonapp.engine.api.TextureRegion;
import com.hsw.etalonapp.engine.loop.defaults.SimpleWorldState;

import org.junit.Test;

import java.util.Collections;

public class SimpleWorldStateSaveMapperTest {

    private static final float FLOAT_DELTA = 0.0001f;

    @Test
    public void roundTrip_preservesCurrentEntityFields() {
        SimpleWorldState.EntityState originalEntity = new SimpleWorldState.EntityState(42,
                7,
                SimpleWorldState.EntityState.ControlMode.HUMAN_TOUCH,
                11,
                0.25f,
                -0.15f,
                0.75f,
                33.0f,
                0.8f,
                0.12f,
                -0.34f,
                21.0f,
                1.25f,
                0.22f,
                BlendMode.ADDITIVE,
                2,
                5,
                ScissorRect.of(1, 2, 3, 4),
                new TextureRegion(0.1f, 0.2f, 0.3f, 0.4f),
                0.6f,
                1.4f);
        SimpleWorldState originalWorld = new SimpleWorldState(Collections.singletonList(originalEntity));

        SimpleWorldStateSaveMapper mapper = new SimpleWorldStateSaveMapper();
        SimpleWorldState restoredWorld = mapper.fromSave(mapper.toSave(originalWorld));

        assertEquals(1, restoredWorld.getEntities().size());
        assertEntityEquals(originalEntity, restoredWorld.getEntities().get(0));
    }

    private static void assertEntityEquals(SimpleWorldState.EntityState expected,
                                           SimpleWorldState.EntityState actual) {
        assertEquals(expected.getEntityId(), actual.getEntityId());
        assertEquals(expected.getOwnerId(), actual.getOwnerId());
        assertEquals(expected.getControlMode(), actual.getControlMode());
        assertEquals(expected.getTextureSlot(), actual.getTextureSlot());
        assertEquals(expected.getBlendMode(), actual.getBlendMode());
        assertEquals(expected.getLayer(), actual.getLayer());
        assertEquals(expected.getRenderOrder(), actual.getRenderOrder());
        assertEquals(expected.getX(), actual.getX(), FLOAT_DELTA);
        assertEquals(expected.getY(), actual.getY(), FLOAT_DELTA);
        assertEquals(expected.getZ(), actual.getZ(), FLOAT_DELTA);
        assertEquals(expected.getScaleX(), actual.getScaleX(), FLOAT_DELTA);
        assertEquals(expected.getScaleY(), actual.getScaleY(), FLOAT_DELTA);
        assertEquals(expected.getRotationDeg(), actual.getRotationDeg(), FLOAT_DELTA);
        assertEquals(expected.getAnimationState(), actual.getAnimationState(), FLOAT_DELTA);
        assertEquals(expected.getVelocityX(), actual.getVelocityX(), FLOAT_DELTA);
        assertEquals(expected.getVelocityY(), actual.getVelocityY(), FLOAT_DELTA);
        assertEquals(expected.getAngularVelocityDeg(), actual.getAngularVelocityDeg(), FLOAT_DELTA);
        assertEquals(expected.getAnimationSpeed(), actual.getAnimationSpeed(), FLOAT_DELTA);
        assertEquals(expected.getCollisionRadius(), actual.getCollisionRadius(), FLOAT_DELTA);
        assertScissorEquals(expected.getScissorRect(), actual.getScissorRect());
        assertTextureRegionEquals(expected.getTextureRegion(), actual.getTextureRegion());
    }

    private static void assertScissorEquals(ScissorRect expected, ScissorRect actual) {
        assertEquals(expected.isEnabled(), actual.isEnabled());
        assertEquals(expected.getX(), actual.getX());
        assertEquals(expected.getY(), actual.getY());
        assertEquals(expected.getWidth(), actual.getWidth());
        assertEquals(expected.getHeight(), actual.getHeight());
    }

    private static void assertTextureRegionEquals(TextureRegion expected, TextureRegion actual) {
        assertEquals(expected.getU(), actual.getU(), FLOAT_DELTA);
        assertEquals(expected.getV(), actual.getV(), FLOAT_DELTA);
        assertEquals(expected.getWidthUv(), actual.getWidthUv(), FLOAT_DELTA);
        assertEquals(expected.getHeightUv(), actual.getHeightUv(), FLOAT_DELTA);
    }
}
