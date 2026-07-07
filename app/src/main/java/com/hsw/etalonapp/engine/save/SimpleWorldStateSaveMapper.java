package com.hsw.etalonapp.engine.save;

import com.hsw.etalonapp.engine.api.BlendMode;
import com.hsw.etalonapp.engine.api.ScissorRect;
import com.hsw.etalonapp.engine.api.TextureRegion;
import com.hsw.etalonapp.engine.loop.defaults.SimpleWorldState;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

final class SimpleWorldStateSaveMapper {

    private static final String WORLD_ID = "simple_world";

    GameSaveDto toSave(SimpleWorldState worldState) {
        Objects.requireNonNull(worldState, "worldState");
        List<SavedEntityDto> entities = new ArrayList<>();
        for (SimpleWorldState.EntityState entity : worldState.getEntities()) {
            ScissorRect scissor = entity.getScissorRect();
            TextureRegion textureRegion = entity.getTextureRegion();
            entities.add(new SavedEntityDto(entity.getEntityId(),
                    entity.getOwnerId(),
                    entity.getControlMode().name(),
                    entity.getTextureSlot(),
                    entity.getBlendMode().name(),
                    entity.getLayer(),
                    entity.getRenderOrder(),
                    new SavedScissorDto(scissor.isEnabled(),
                            scissor.getX(),
                            scissor.getY(),
                            scissor.getWidth(),
                            scissor.getHeight()),
                    new SavedTextureRegionDto(textureRegion.getU(),
                            textureRegion.getV(),
                            textureRegion.getWidthUv(),
                            textureRegion.getHeightUv()),
                    entity.getX(),
                    entity.getY(),
                    entity.getZ(),
                    entity.getScaleX(),
                    entity.getScaleY(),
                    entity.getRotationDeg(),
                    entity.getAnimationState(),
                    entity.getVelocityX(),
                    entity.getVelocityY(),
                    entity.getAngularVelocityDeg(),
                    entity.getAnimationSpeed(),
                    entity.getCollisionRadius()));
        }
        return new GameSaveDto(GameSaveDto.SUPPORTED_SCHEMA_VERSION, WORLD_ID, entities);
    }

    SimpleWorldState fromSave(GameSaveDto saveDto) {
        Objects.requireNonNull(saveDto, "saveDto");
        if (saveDto.schemaVersion != GameSaveDto.SUPPORTED_SCHEMA_VERSION) {
            throw new IllegalArgumentException("Unsupported save schema version: " + saveDto.schemaVersion);
        }
        if (!WORLD_ID.equals(saveDto.worldId)) {
            throw new IllegalArgumentException("Unsupported save world id: " + saveDto.worldId);
        }

        List<SimpleWorldState.EntityState> entities = new ArrayList<>();
        for (SavedEntityDto savedEntity : saveDto.entities) {
            entities.add(toEntityState(savedEntity));
        }
        return new SimpleWorldState(entities);
    }

    private static SimpleWorldState.EntityState toEntityState(SavedEntityDto savedEntity) {
        SavedScissorDto savedScissor = savedEntity.scissor;
        SavedTextureRegionDto savedTextureRegion = savedEntity.textureRegion;
        ScissorRect scissor = savedScissor.enabled
                ? ScissorRect.of(savedScissor.x, savedScissor.y, savedScissor.width, savedScissor.height)
                : ScissorRect.disabled();
        TextureRegion textureRegion = new TextureRegion(savedTextureRegion.u,
                savedTextureRegion.v,
                savedTextureRegion.widthUv,
                savedTextureRegion.heightUv);

        return new SimpleWorldState.EntityState(savedEntity.entityId,
                savedEntity.ownerId,
                SimpleWorldState.EntityState.ControlMode.valueOf(savedEntity.controlMode),
                savedEntity.textureSlot,
                savedEntity.x,
                savedEntity.y,
                savedEntity.z,
                savedEntity.rotationDeg,
                savedEntity.animationState,
                savedEntity.velocityX,
                savedEntity.velocityY,
                savedEntity.angularVelocityDeg,
                savedEntity.animationSpeed,
                savedEntity.collisionRadius,
                BlendMode.valueOf(savedEntity.blendMode),
                savedEntity.layer,
                savedEntity.renderOrder,
                scissor,
                textureRegion,
                savedEntity.scaleX,
                savedEntity.scaleY);
    }
}
