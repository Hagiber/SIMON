package com.hsw.simonapp.engine.scene;

import com.hsw.simonapp.engine.scene.TextureCatalog;
import com.hsw.simonapp.engine.loop.core.RenderFrameState;
import com.hsw.simonapp.engine.loop.core.WorldState;
import com.hsw.simonapp.engine.loop.defaults.SimpleWorldState;
import com.hsw.simonapp.engine.api.BlendMode;
import com.hsw.simonapp.engine.api.OrthoCamera;
import com.hsw.simonapp.engine.api.ScissorRect;
import com.hsw.simonapp.engine.api.SceneFrame;
import com.hsw.simonapp.engine.api.SceneSnapshot;
import com.hsw.simonapp.engine.api.TextureRegion;

import java.util.List;

public final class NativeSceneSnapshotFactory {

    private static final int FIRE_ATLAS_COLUMNS = 5;
    private static final int FIRE_ATLAS_ROWS = 5;
    private static final int FIRE_ATLAS_FRAME_COUNT = FIRE_ATLAS_COLUMNS * FIRE_ATLAS_ROWS;

    private volatile OrthoCamera camera = OrthoCamera.defaults();

    public void setCamera(OrthoCamera camera) {
        this.camera = java.util.Objects.requireNonNull(camera, "camera");
    }

    public SceneFrame create(RenderFrameState renderFrameState) {
        SceneSnapshot previousSceneSnapshot = createSnapshot(renderFrameState.getPreviousWorldState());
        SceneSnapshot currentSceneSnapshot = createSnapshot(renderFrameState.getCurrentWorldState());
        if (previousSceneSnapshot.size() != currentSceneSnapshot.size()) {
            return SceneFrame.empty(renderFrameState.getInterpolationAlpha());
        }
        return SceneFrame.of(previousSceneSnapshot,
                currentSceneSnapshot,
                camera,
                camera,
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
        if (entity.getTextureSlot() == TextureCatalog.RED_BUTTON_TEXTURE_SLOT
                || entity.getTextureSlot() == TextureCatalog.GREEN_BUTTON_TEXTURE_SLOT) {
            return resolveButtonPressTextureRegion(entity);
        }

        if (entity.getTextureSlot() != TextureCatalog.FIRE_ATLAS_TEXTURE_SLOT) {
            return entity.getTextureRegion();
        }

        int frameIndex = (int) Math.floor(entity.getAnimationState());
        return atlasTextureRegion(frameIndex, FIRE_ATLAS_COLUMNS, FIRE_ATLAS_ROWS, FIRE_ATLAS_FRAME_COUNT);
    }

    private static TextureRegion resolveButtonPressTextureRegion(SimpleWorldState.EntityState entity) {
        float activeAnimationState = Math.max(0.0f, Math.min(1.0f, entity.getAnimationState()));
        float progress = 1.0f - activeAnimationState;
        int frameIndex = (int) Math.floor(progress * TextureCatalog.RED_BUTTON_PRESS_ATLAS_FRAME_COUNT);
        return atlasTextureRegion(frameIndex,
                TextureCatalog.RED_BUTTON_PRESS_ATLAS_COLUMNS,
                TextureCatalog.RED_BUTTON_PRESS_ATLAS_ROWS,
                TextureCatalog.RED_BUTTON_PRESS_ATLAS_FRAME_COUNT);
    }

    private static TextureRegion atlasTextureRegion(int frameIndex, int columns, int rows, int frameCount) {
        int wrappedFrameIndex = Math.floorMod(frameIndex, frameCount);
        int column = wrappedFrameIndex % columns;
        int row = wrappedFrameIndex / columns;

        float widthUv = 1.0f / columns;
        float heightUv = 1.0f / rows;
        float u = column * widthUv;
        float v = row * heightUv;
        return new TextureRegion(u, v, widthUv, heightUv);
    }
}
