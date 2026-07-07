package com.hsw.simonapp.engine.render;

import com.hsw.simonapp.engine.loop.core.FrameContext;

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
