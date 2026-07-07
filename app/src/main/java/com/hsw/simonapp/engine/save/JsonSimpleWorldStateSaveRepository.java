package com.hsw.simonapp.engine.save;

import com.hsw.simonapp.engine.loop.defaults.SimpleWorldState;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

public final class JsonSimpleWorldStateSaveRepository {

    private final SimpleWorldStateSaveMapper mapper = new SimpleWorldStateSaveMapper();

    public void save(File saveFile, SimpleWorldState worldState) throws IOException {
        Objects.requireNonNull(saveFile, "saveFile");
        Objects.requireNonNull(worldState, "worldState");
        GameSaveDto saveDto = mapper.toSave(worldState);
        String content;
        try {
            content = JsonGameSaveCodec.toJson(saveDto).toString(2);
        } catch (JSONException e) {
            throw new IOException("Failed to encode world save JSON", e);
        }
        writeAtomically(saveFile, content);
    }

    public SimpleWorldState load(File saveFile) throws IOException {
        Objects.requireNonNull(saveFile, "saveFile");
        if (!saveFile.isFile()) {
            throw new IOException("Save file does not exist: " + saveFile);
        }

        try {
            JSONObject json = new JSONObject(readUtf8(saveFile));
            return mapper.fromSave(JsonGameSaveCodec.fromJson(json));
        } catch (JSONException | IllegalArgumentException e) {
            throw new IOException("Failed to decode world save JSON", e);
        }
    }

    private static void writeAtomically(File saveFile, String content) throws IOException {
        File parent = saveFile.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new IOException("Failed to create save directory: " + parent);
        }

        File tempFile = File.createTempFile(saveFile.getName(), ".tmp", parent);
        boolean renamed = false;
        try (FileOutputStream outputStream = new FileOutputStream(tempFile)) {
            outputStream.write(content.getBytes(StandardCharsets.UTF_8));
        }

        if (saveFile.exists() && !saveFile.delete()) {
            throw new IOException("Failed to replace save file: " + saveFile);
        }
        renamed = tempFile.renameTo(saveFile);
        if (!renamed) {
            throw new IOException("Failed to move save file into place: " + saveFile);
        }
    }

    private static String readUtf8(File file) throws IOException {
        try (FileInputStream inputStream = new FileInputStream(file);
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, read);
            }
            return outputStream.toString(StandardCharsets.UTF_8.name());
        }
    }
}
