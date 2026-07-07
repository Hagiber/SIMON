package com.hsw.simonapp.engine.scene;

import static org.junit.Assert.assertArrayEquals;

import com.hsw.simonapp.engine.loop.core.RenderFrameState;
import com.hsw.simonapp.engine.loop.core.WorldState;
import com.hsw.simonapp.engine.loop.defaults.SimpleWorldState;
import com.hsw.simonapp.engine.api.BlendMode;
import com.hsw.simonapp.engine.api.ScissorRect;
import com.hsw.simonapp.engine.api.SceneFrame;
import com.hsw.simonapp.engine.api.SceneSnapshot;
import com.hsw.simonapp.engine.api.TextureRegion;

import org.junit.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.Arrays;

public class NativeSceneSnapshotFactoryTest {

    @Test
    public void create_preservesDefaultSpriteScale() throws Exception {
        SimpleWorldState worldState = new SimpleWorldState(Arrays.asList(
                new SimpleWorldState.EntityState(1,
                        0,
                        SimpleWorldState.EntityState.ControlMode.AI,
                        0,
                        0f,
                        0f,
                        1f,
                        0f,
                        0f,
                        0f,
                        0f,
                        0.14f)
        ));

        SceneFrame sceneFrame = new NativeSceneSnapshotFactory()
                .create(renderFrameState(worldState, worldState, 0.0f));
        SceneSnapshot currentSnapshot = sceneSnapshot(sceneFrame, "currentSceneSnapshot");

        assertArrayEquals(new float[]{1.0f}, floatArray(currentSnapshot, "scaleXs"), 0.0001f);
        assertArrayEquals(new float[]{1.0f}, floatArray(currentSnapshot, "scaleYs"), 0.0001f);
    }

    @Test
    public void create_preservesExplicitEntityScale() throws Exception {
        SimpleWorldState worldState = new SimpleWorldState(Arrays.asList(
                new SimpleWorldState.EntityState(1,
                        0,
                        SimpleWorldState.EntityState.ControlMode.AI,
                        0,
                        0f,
                        0f,
                        1f,
                        0f,
                        0f,
                        0f,
                        0f,
                        0f,
                        0f,
                        0.14f,
                        BlendMode.ALPHA,
                        0,
                        0,
                        ScissorRect.disabled(),
                        TextureRegion.full(),
                        1.75f,
                        0.45f)
        ));

        SceneFrame sceneFrame = new NativeSceneSnapshotFactory()
                .create(renderFrameState(worldState, worldState, 0.0f));
        SceneSnapshot currentSnapshot = sceneSnapshot(sceneFrame, "currentSceneSnapshot");

        assertArrayEquals(new float[]{1.75f}, floatArray(currentSnapshot, "scaleXs"), 0.0001f);
        assertArrayEquals(new float[]{0.45f}, floatArray(currentSnapshot, "scaleYs"), 0.0001f);
    }

    @Test
    public void create_flipsWorldYForVulkanCoordinates() throws Exception {
        SimpleWorldState worldState = new SimpleWorldState(Arrays.asList(
                new SimpleWorldState.EntityState(1,
                        0,
                        SimpleWorldState.EntityState.ControlMode.AI,
                        0,
                        0f,
                        0.25f,
                        1f,
                        0f,
                        0f,
                        0f,
                        0f,
                        0.14f)
        ));

        SceneFrame sceneFrame = new NativeSceneSnapshotFactory()
                .create(renderFrameState(worldState, worldState, 0.0f));
        SceneSnapshot currentSnapshot = sceneSnapshot(sceneFrame, "currentSceneSnapshot");

        assertArrayEquals(new float[]{-0.25f}, floatArray(currentSnapshot, "ys"), 0.0001f);
    }

    private static RenderFrameState renderFrameState(WorldState previousWorldState,
                                                     WorldState currentWorldState,
                                                     float interpolationAlpha) throws Exception {
        Constructor<RenderFrameState> constructor = RenderFrameState.class.getDeclaredConstructor(
                WorldState.class,
                WorldState.class,
                float.class);
        constructor.setAccessible(true);
        return constructor.newInstance(previousWorldState, currentWorldState, interpolationAlpha);
    }

    private static SceneSnapshot sceneSnapshot(SceneFrame sceneFrame, String fieldName) throws Exception {
        Field field = SceneFrame.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        return (SceneSnapshot) field.get(sceneFrame);
    }

    private static float[] floatArray(SceneSnapshot sceneSnapshot, String fieldName) throws Exception {
        Field field = SceneSnapshot.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        return (float[]) field.get(sceneSnapshot);
    }
}
