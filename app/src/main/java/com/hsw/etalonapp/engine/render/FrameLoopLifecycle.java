package com.hsw.etalonapp.engine.render;

import com.hsw.etalonapp.engine.loop.core.FrameContext;

final class FrameLoopLifecycle {

    private final RenderLoopController renderLoopController;

    FrameLoopLifecycle(RenderLoopController renderLoopController) {
        this.renderLoopController = renderLoopController;
    }

    void start() {
        renderLoopController.start();
    }

    void stop() {
        renderLoopController.stop();
    }

    void tick(FrameContext frameContext) {
        renderLoopController.tick(frameContext);
    }
}
