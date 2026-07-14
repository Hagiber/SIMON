package com.hsw.simonapp.engine.render;

import com.hsw.simonapp.engine.api.OrthoCamera;

interface ViewportAwareFrameRenderer {
    void setCamera(OrthoCamera camera);
}
