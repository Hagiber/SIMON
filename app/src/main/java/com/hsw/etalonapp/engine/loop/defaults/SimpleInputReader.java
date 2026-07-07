package com.hsw.etalonapp.engine.loop.defaults;

import com.hsw.etalonapp.engine.loop.core.FrameContext;
import com.hsw.etalonapp.engine.loop.core.InputReader;
import com.hsw.etalonapp.engine.loop.core.InputSnapshot;
import com.hsw.etalonapp.engine.loop.core.NeutralInputSnapshot;

public final class SimpleInputReader implements InputReader {
    @Override
    public InputSnapshot read(FrameContext frameContext) {
        return NeutralInputSnapshot.INSTANCE;
    }
}


