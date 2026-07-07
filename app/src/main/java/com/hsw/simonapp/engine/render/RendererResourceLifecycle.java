package com.hsw.simonapp.engine.render;

import android.app.Activity;
import android.view.Surface;

final class RendererResourceLifecycle {

    private final VulkanRendererSession rendererSession;

    RendererResourceLifecycle(VulkanRendererSession rendererSession) {
        this.rendererSession = rendererSession;
    }

    VulkanRenderCoordinator.CoordinatorResult init(Activity activity, Surface surface) {
        return rendererSession.ensureInitialized(activity, surface);
    }

    void resize(int width, int height) {
        rendererSession.resize(width, height);
    }

    void release() {
        rendererSession.release();
    }

    boolean isInitialized() {
        return rendererSession.isInitialized();
    }

    String getLastVulkanError() {
        return rendererSession.getLastVulkanError();
    }
}
