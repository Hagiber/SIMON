package com.hsw.simonapp.engine.scene.layout;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.hsw.simonapp.engine.api.OrthoCamera;
import com.hsw.simonapp.engine.loop.defaults.SimpleWorldState;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

public class SceneLayoutTest {
    private static final float FLOAT_DELTA = 0.0001f;

    @Test
    public void calculateSpriteLayout_fitWidthUsesVisibleWorldWidth() {
        SpriteLayout layout = SceneLayout.calculateSpriteLayout(1080,
                1920,
                TextureDimensions.of(168, 62),
                FitMode.FIT_WIDTH,
                0.96f);

        assertEquals(2.742857f, layout.getScaleX(), FLOAT_DELTA);
        assertEquals(2.742857f, layout.getScaleY(), FLOAT_DELTA);
    }

    @Test
    public void calculateSpriteLayout_coverIsLargerThanContainForWideTextureInPortrait() {
        TextureDimensions textureDimensions = TextureDimensions.of(168, 62);

        SpriteLayout contain = SceneLayout.calculateSpriteLayout(1080,
                1920,
                textureDimensions,
                FitMode.CONTAIN,
                0.96f);
        SpriteLayout cover = SceneLayout.calculateSpriteLayout(1080,
                1920,
                textureDimensions,
                FitMode.COVER,
                0.96f);

        assertEquals(2.742857f, contain.getScaleX(), FLOAT_DELTA);
        assertTrue(cover.getScaleX() > contain.getScaleX());
        assertEquals(cover.getScaleX(), cover.getScaleY(), FLOAT_DELTA);
    }

    @Test
    public void cameraLayoutPolicy_fitHeightKeepsReferenceHeightOnTallPortraitViewport() {
        CameraLayoutPolicy policy = new CameraLayoutPolicy(2.0f,
                3.5555556f,
                FitMode.FIT_HEIGHT);

        OrthoCamera camera = policy.cameraForViewport(1080, 2400);

        assertEquals(1.25f, camera.getZoom(), FLOAT_DELTA);
    }

    @Test
    public void calculateSpriteLayout_usesCameraZoomForVisibleWorldWidth() {
        OrthoCamera camera = OrthoCamera.of(0.0f, 0.0f, 1.25f, 0.0f);

        SpriteLayout layout = SceneLayout.calculateSpriteLayout(1080,
                2400,
                TextureDimensions.of(168, 62),
                FitMode.FIT_WIDTH,
                0.96f,
                camera);

        assertEquals(2.194286f, layout.getScaleX(), FLOAT_DELTA);
        assertEquals(2.194286f, layout.getScaleY(), FLOAT_DELTA);
    }

    @Test
    public void applyUpdatesOnlyEntitiesWithMatchingTextureSlot() {
        SimpleWorldState worldState = new SimpleWorldState(Arrays.asList(
                createEntity(1, 0),
                createEntity(2, 1)
        ));
        SceneLayout sceneLayout = new SceneLayout(slot -> TextureDimensions.of(168, 62),
                Collections.singletonList(new SpriteLayoutRule(0, FitMode.FIT_WIDTH, 0.96f)));

        sceneLayout.apply(worldState, 1080, 1920);

        SimpleWorldState.EntityState fittedEntity = worldState.findEntityById(1);
        SimpleWorldState.EntityState untouchedEntity = worldState.findEntityById(2);
        assertEquals(2.742857f, fittedEntity.getScaleX(), FLOAT_DELTA);
        assertEquals(2.742857f, fittedEntity.getScaleY(), FLOAT_DELTA);
        assertEquals(1.0f, untouchedEntity.getScaleX(), FLOAT_DELTA);
        assertEquals(1.0f, untouchedEntity.getScaleY(), FLOAT_DELTA);
    }

    @Test
    public void applyUsesRuleScaleMultipliers() {
        SimpleWorldState worldState = new SimpleWorldState(Collections.singletonList(
                createEntity(1, 0)
        ));
        SceneLayout sceneLayout = new SceneLayout(slot -> TextureDimensions.of(168, 62),
                Collections.singletonList(new SpriteLayoutRule(0,
                        FitMode.FIT_WIDTH,
                        0.96f,
                        1.10f,
                        0.70f)));

        sceneLayout.apply(worldState, 1080, 1920);

        SimpleWorldState.EntityState entity = worldState.findEntityById(1);
        assertEquals(3.017143f, entity.getScaleX(), FLOAT_DELTA);
        assertEquals(1.92f, entity.getScaleY(), FLOAT_DELTA);
    }

    private static SimpleWorldState.EntityState createEntity(int entityId, int textureSlot) {
        return new SimpleWorldState.EntityState(entityId,
                0,
                SimpleWorldState.EntityState.ControlMode.AI,
                textureSlot,
                0.0f,
                0.0f,
                1.0f,
                0.0f,
                0.0f,
                0.0f,
                0.0f,
                0.1f);
    }
}
