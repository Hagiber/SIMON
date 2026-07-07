package com.hsw.etalonapp.engine.save;

final class SavedTextureRegionDto {

    final float u;
    final float v;
    final float widthUv;
    final float heightUv;

    SavedTextureRegionDto(float u, float v, float widthUv, float heightUv) {
        this.u = u;
        this.v = v;
        this.widthUv = widthUv;
        this.heightUv = heightUv;
    }
}
