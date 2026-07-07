package com.hsw.simonapp.engine.save;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

final class JsonGameSaveCodec {

    private JsonGameSaveCodec() {
    }

    static JSONObject toJson(GameSaveDto saveDto) throws JSONException {
        JSONObject root = new JSONObject();
        root.put("schemaVersion", saveDto.schemaVersion);
        root.put("worldId", saveDto.worldId);

        JSONArray entities = new JSONArray();
        for (SavedEntityDto entity : saveDto.entities) {
            entities.put(entityToJson(entity));
        }
        root.put("entities", entities);
        return root;
    }

    static GameSaveDto fromJson(JSONObject root) throws JSONException {
        int schemaVersion = root.getInt("schemaVersion");
        if (schemaVersion != GameSaveDto.SUPPORTED_SCHEMA_VERSION) {
            throw new JSONException("Unsupported save schema version: " + schemaVersion);
        }

        JSONArray entityArray = root.getJSONArray("entities");
        List<SavedEntityDto> entities = new ArrayList<>();
        for (int i = 0; i < entityArray.length(); i++) {
            entities.add(entityFromJson(entityArray.getJSONObject(i)));
        }
        return new GameSaveDto(schemaVersion, root.optString("worldId", "simple_world"), entities);
    }

    private static JSONObject entityToJson(SavedEntityDto entity) throws JSONException {
        JSONObject json = new JSONObject();
        json.put("entityId", entity.entityId);
        json.put("ownerId", entity.ownerId);
        json.put("controlMode", entity.controlMode);
        json.put("textureSlot", entity.textureSlot);
        json.put("blendMode", entity.blendMode);
        json.put("layer", entity.layer);
        json.put("renderOrder", entity.renderOrder);
        json.put("scissor", scissorToJson(entity.scissor));
        json.put("textureRegion", textureRegionToJson(entity.textureRegion));
        json.put("x", entity.x);
        json.put("y", entity.y);
        json.put("z", entity.z);
        json.put("scaleX", entity.scaleX);
        json.put("scaleY", entity.scaleY);
        json.put("rotationDeg", entity.rotationDeg);
        json.put("animationState", entity.animationState);
        json.put("velocityX", entity.velocityX);
        json.put("velocityY", entity.velocityY);
        json.put("angularVelocityDeg", entity.angularVelocityDeg);
        json.put("animationSpeed", entity.animationSpeed);
        json.put("collisionRadius", entity.collisionRadius);
        return json;
    }

    private static SavedEntityDto entityFromJson(JSONObject json) throws JSONException {
        return new SavedEntityDto(json.getInt("entityId"),
                json.getInt("ownerId"),
                json.getString("controlMode"),
                json.getInt("textureSlot"),
                json.getString("blendMode"),
                json.getInt("layer"),
                json.getInt("renderOrder"),
                scissorFromJson(json.getJSONObject("scissor")),
                textureRegionFromJson(json.getJSONObject("textureRegion")),
                (float) json.getDouble("x"),
                (float) json.getDouble("y"),
                (float) json.getDouble("z"),
                (float) json.getDouble("scaleX"),
                (float) json.getDouble("scaleY"),
                (float) json.getDouble("rotationDeg"),
                (float) json.getDouble("animationState"),
                (float) json.getDouble("velocityX"),
                (float) json.getDouble("velocityY"),
                (float) json.getDouble("angularVelocityDeg"),
                (float) json.getDouble("animationSpeed"),
                (float) json.getDouble("collisionRadius"));
    }

    private static JSONObject scissorToJson(SavedScissorDto scissor) throws JSONException {
        JSONObject json = new JSONObject();
        json.put("enabled", scissor.enabled);
        json.put("x", scissor.x);
        json.put("y", scissor.y);
        json.put("width", scissor.width);
        json.put("height", scissor.height);
        return json;
    }

    private static SavedScissorDto scissorFromJson(JSONObject json) throws JSONException {
        return new SavedScissorDto(json.getBoolean("enabled"),
                json.getInt("x"),
                json.getInt("y"),
                json.getInt("width"),
                json.getInt("height"));
    }

    private static JSONObject textureRegionToJson(SavedTextureRegionDto textureRegion) throws JSONException {
        JSONObject json = new JSONObject();
        json.put("u", textureRegion.u);
        json.put("v", textureRegion.v);
        json.put("widthUv", textureRegion.widthUv);
        json.put("heightUv", textureRegion.heightUv);
        return json;
    }

    private static SavedTextureRegionDto textureRegionFromJson(JSONObject json) throws JSONException {
        return new SavedTextureRegionDto((float) json.getDouble("u"),
                (float) json.getDouble("v"),
                (float) json.getDouble("widthUv"),
                (float) json.getDouble("heightUv"));
    }
}
