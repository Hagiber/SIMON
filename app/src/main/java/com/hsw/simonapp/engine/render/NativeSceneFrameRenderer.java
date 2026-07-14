package com.hsw.simonapp.engine.render;

import com.hsw.simonapp.engine.scene.NativeSceneSnapshotFactory;
import com.hsw.simonapp.engine.api.OrthoCamera;
import com.hsw.simonapp.engine.loop.core.FrameContext;
import com.hsw.simonapp.engine.loop.core.FrameRenderer;
import com.hsw.simonapp.engine.loop.core.FrameTimingAccumulator;
import com.hsw.simonapp.engine.loop.core.RenderFrameState;
import com.hsw.simonapp.engine.api.NativeLib;
import com.hsw.simonapp.engine.api.SceneFrame;

import java.util.Objects;
import java.util.function.BiConsumer;

final class NativeSceneFrameRenderer implements FrameRenderer, ViewportAwareFrameRenderer {

    private final NativeLib nativeLib;
    private final NativeSceneSnapshotFactory sceneSnapshotFactory;
    private final BiConsumer<VulkanRenderCoordinator.CoordinatorErrorCode, String> failureSink;

    NativeSceneFrameRenderer(NativeLib nativeLib,
                             NativeSceneSnapshotFactory sceneSnapshotFactory,
                             BiConsumer<VulkanRenderCoordinator.CoordinatorErrorCode, String> failureSink) {
        this.nativeLib = Objects.requireNonNull(nativeLib, "nativeLib");
        this.sceneSnapshotFactory = Objects.requireNonNull(sceneSnapshotFactory, "sceneSnapshotFactory");
        this.failureSink = Objects.requireNonNull(failureSink, "failureSink");
    }

    @Override
    public void setCamera(OrthoCamera camera) {
        sceneSnapshotFactory.setCamera(camera);
    }

    @Override
    public void render(FrameContext frameContext, RenderFrameState renderFrameState) {
        render(frameContext, renderFrameState, new FrameTimingAccumulator());
    }

    @Override
    public void render(FrameContext frameContext,
                       RenderFrameState renderFrameState,
                       FrameTimingAccumulator timingAccumulator) {
        FrameTimingAccumulator nonNullTimingAccumulator =
                Objects.requireNonNull(timingAccumulator, "timingAccumulator");
        long snapshotStartNanos = System.nanoTime();
        SceneFrame sceneFrame = sceneSnapshotFactory.create(renderFrameState);
        nonNullTimingAccumulator.addSnapshotBuildNanos(System.nanoTime() - snapshotStartNanos);

        long nativeRenderStartNanos = System.nanoTime();
        if (!nativeLib.renderSceneFrame(sceneFrame)) {
            failureSink.accept(VulkanRenderCoordinator.CoordinatorErrorCode.SCENE_FRAME_RENDER_FAILED,
                    "renderSceneFrame failed: " + nativeLib.getLastVulkanError());
        }
        nonNullTimingAccumulator.addNativeRenderNanos(System.nanoTime() - nativeRenderStartNanos);
    }
}
