package com.hsw.simonapp.engine.save;

final class SavedScissorDto {

    final boolean enabled;
    final int x;
    final int y;
    final int width;
    final int height;

    SavedScissorDto(boolean enabled, int x, int y, int width, int height) {
        this.enabled = enabled;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }
}
