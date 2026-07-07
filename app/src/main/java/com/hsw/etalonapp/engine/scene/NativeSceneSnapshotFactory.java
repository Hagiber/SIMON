package com.hsw.etalonapp.engine.scene;

import com.hsw.etalonapp.engine.scene.TextureCatalog;
import com.hsw.etalonapp.engine.loop.core.RenderFrameState;
import com.hsw.etalonapp.engine.loop.core.WorldState;
import com.hsw.etalonapp.engine.loop.defaults.SimpleWorldState;
import com.hsw.etalonapp.engine.api.BlendMode;
import com.hsw.etalonapp.engine.api.ScissorRect;
import com.hsw.etalonapp.engine.api.SceneFrame;
import com.hsw.etalonapp.engine.api.SceneSnapshot;
import com.hsw.etalonapp.engine.api.TextureRegion;

import java.util.List;

public final class NativeSceneSnapshotFactory {

    private static final int FIRE_ATLAS_COLUMNS = 5;
    private static final int FIRE_ATLAS_ROWS = 5;
    private static final int FIRE_ATLAS_FRAME_COUNT = FIRE_ATLAS_COLUMNS * FIRE_ATLAS_ROWS;

    public SceneFrame create(RenderFrameState renderFrameState) {
        SceneSnapshot previousSceneSnapshot = createSnapshot(renderFrameState.getPreviousWorldState());
        SceneSnapshot currentSceneSnapshot = createSnapshot(renderFrameState.getCurrentWorldState());
        if (previousSceneSnapshot.size() != currentSceneSnapshot.size()) {
            return SceneFrame.empty(renderFrameState.getInterpolationAlpha());
        }
        return SceneFrame.of(previousSceneSnapshot,
                currentSceneSnapshot,
                renderFrameState.getInterpolationAlpha());
    }

    private SceneSnapshot createSnapshot(WorldState worldState) {
        if (!(worldState instanceof SimpleWorldState)) {
            return SceneSnapshot.empty();
        }

        List<SimpleWorldState.EntityState> entities = ((SimpleWorldState) worldState).getEntities();
        int count = entities.size();
        if (count == 0) {
            return SceneSnapshot.empty();
        }

        int[] textureSlots = new int[count];
        BlendMode[] blendModes = new BlendMode[count];
        int[] layers = new int[count];
        int[] renderOrders = new int[count];
        ScissorRect[] scissorRects = new ScissorRect[count];
        TextureRegion[] textureRegions = new TextureRegion[count];
        float[] xs = new float[count];
        float[] ys = new float[count];
        float[] zs = new float[count];
        float[] scaleXs = new float[count];
        float[] scaleYs = new float[count];
        float[] rotationDegs = new float[count];
        float[] animationStates = new float[count];

        for (int i = 0; i < count; i++) {
            SimpleWorldState.EntityState entity = entities.get(i);
            textureSlots[i] = entity.getTextureSlot();
            blendModes[i] = entity.getBlendMode();
            layers[i] = entity.getLayer();
            renderOrders[i] = entity.getRenderOrder();
            scissorRects[i] = entity.getScissorRect();
            textureRegions[i] = resolveTextureRegion(entity);
            xs[i] = entity.getX();
            ys[i] = -entity.getY();
            zs[i] = entity.getZ();
            scaleXs[i] = entity.getScaleX();
            scaleYs[i] = entity.getScaleY();
            rotationDegs[i] = entity.getRotationDeg();
            animationStates[i] = entity.getAnimationState();
        }

        return SceneSnapshot.of(textureSlots,
                xs,
                ys,
                zs,
                scaleXs,
                scaleYs,
                rotationDegs,
                animationStates,
                blendModes,
                layers,
                renderOrders,
                scissorRects,
                textureRegions);
    }

    private TextureRegion resolveTextureRegion(SimpleWorldState.EntityState entity) {
        if (entity.getTextureSlot() != TextureCatalog.FIRE_ATLAS_TEXTURE_SLOT) {
            return entity.getTextureRegion();
        }

        int frameIndex = (int) Math.floor(entity.getAnimationState());
        int wrappedFrameIndex = Math.floorMod(frameIndex, FIRE_ATLAS_FRAME_COUNT);
        int column = wrappedFrameIndex % FIRE_ATLAS_COLUMNS;
        int row = wrappedFrameIndex / FIRE_ATLAS_COLUMNS;

        float widthUv = 1.0f / FIRE_ATLAS_COLUMNS;
        float heightUv = 1.0f / FIRE_ATLAS_ROWS;
        float u = column * widthUv;
        float v = row * heightUv;
        return new TextureRegion(u, v, widthUv, heightUv);
    }
}
