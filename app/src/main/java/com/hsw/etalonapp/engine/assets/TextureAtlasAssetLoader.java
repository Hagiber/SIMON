package com.hsw.etalonapp.engine.assets;

import android.content.res.AssetManager;

import com.hsw.etalonapp.engine.scene.model.TextureAtlas;
import com.hsw.etalonapp.engine.api.TextureRegion;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class TextureAtlasAssetLoader {

    private final AssetManager assetManager;

    public TextureAtlasAssetLoader(AssetManager assetManager) {
        this.assetManager = Objects.requireNonNull(assetManager, "assetManager");
    }

    public TextureAtlas load(String metadataAssetName) throws IOException {
        Objects.requireNonNull(metadataAssetName, "metadataAssetName");
        try {
            JSONObject root = new JSONObject(readAssetText(metadataAssetName));
            JSONArray framesJson = root.getJSONArray("frames");
            List<TextureAtlas.Frame> frames = new ArrayList<>(framesJson.length());
            for (int i = 0; i < framesJson.length(); i++) {
                JSONObject frameJson = framesJson.getJSONObject(i);
                TextureRegion textureRegion = new TextureRegion(
                        (float) frameJson.getDouble("u"),
                        (float) frameJson.getDouble("v"),
                        (float) frameJson.getDouble("widthUv"),
                        (float) frameJson.getDouble("heightUv"));
                frames.add(new TextureAtlas.Frame(
                        frameJson.getString("name"),
                        frameJson.getInt("index"),
                        frameJson.getInt("x"),
                        frameJson.getInt("y"),
                        frameJson.getInt("width"),
                        frameJson.getInt("height"),
                        textureRegion));
            }

            return new TextureAtlas(
                    root.getString("image"),
                    root.getInt("width"),
                    root.getInt("height"),
                    root.getInt("columns"),
                    root.getInt("rows"),
                    frames);
        } catch (JSONException | IllegalArgumentException e) {
            throw new IOException("Invalid texture atlas metadata: " + metadataAssetName, e);
        }
    }

    private String readAssetText(String assetName) throws IOException {
        try (InputStream inputStream = assetManager.open(assetName);
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, read);
            }
            return new String(outputStream.toByteArray(), StandardCharsets.UTF_8);
        }
    }
}
