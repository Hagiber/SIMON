package com.hsw.etalonapp.engine.scene.model;

import com.hsw.etalonapp.engine.api.TextureRegion;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class TextureAtlas {

    private final String imageAssetName;
    private final int width;
    private final int height;
    private final int columns;
    private final int rows;
    private final List<Frame> frames;

    public TextureAtlas(String imageAssetName,
                        int width,
                        int height,
                        int columns,
                        int rows,
                        List<Frame> frames) {
        this.imageAssetName = Objects.requireNonNull(imageAssetName, "imageAssetName");
        if (width <= 0 || height <= 0 || columns <= 0 || rows <= 0) {
            throw new IllegalArgumentException("TextureAtlas dimensions and grid must be positive");
        }
        this.width = width;
        this.height = height;
        this.columns = columns;
        this.rows = rows;
        this.frames = Collections.unmodifiableList(new ArrayList<>(
                Objects.requireNonNull(frames, "frames")));
        if (this.frames.isEmpty()) {
            throw new IllegalArgumentException("TextureAtlas must contain at least one frame");
        }
    }

    public String getImageAssetName() {
        return imageAssetName;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getColumns() {
        return columns;
    }

    public int getRows() {
        return rows;
    }

    public int frameCount() {
        return frames.size();
    }

    public Frame getFrame(int index) {
        if (index < 0 || index >= frames.size()) {
            throw new IllegalArgumentException("TextureAtlas frame index is out of range: " + index);
        }
        return frames.get(index);
    }

    public List<Frame> getFrames() {
        return frames;
    }

    public static final class Frame {
        private final String name;
        private final int index;
        private final int x;
        private final int y;
        private final int width;
        private final int height;
        private final TextureRegion textureRegion;

        public Frame(String name,
                     int index,
                     int x,
                     int y,
                     int width,
                     int height,
                     TextureRegion textureRegion) {
            this.name = Objects.requireNonNull(name, "name");
            if (index < 0 || x < 0 || y < 0 || width <= 0 || height <= 0) {
                throw new IllegalArgumentException("TextureAtlas frame values are invalid");
            }
            this.index = index;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.textureRegion = Objects.requireNonNull(textureRegion, "textureRegion");
        }

        public String getName() {
            return name;
        }

        public int getIndex() {
            return index;
        }

        public int getX() {
            return x;
        }

        public int getY() {
            return y;
        }

        public int getWidth() {
            return width;
        }

        public int getHeight() {
            return height;
        }

        public TextureRegion getTextureRegion() {
            return textureRegion;
        }
    }
}
