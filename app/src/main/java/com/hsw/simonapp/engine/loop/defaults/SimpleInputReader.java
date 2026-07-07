package com.hsw.simonapp.engine.loop.defaults;

import com.hsw.simonapp.engine.loop.core.FrameContext;
import com.hsw.simonapp.engine.loop.core.InputReader;
import com.hsw.simonapp.engine.loop.core.InputSnapshot;
import com.hsw.simonapp.engine.loop.core.NeutralInputSnapshot;

public final class SimpleInputReader implements InputReader {
    @Override
    public InputSnapshot read(FrameContext frameContext) {
        return NeutralInputSnapshot.INSTANCE;
    }
}


