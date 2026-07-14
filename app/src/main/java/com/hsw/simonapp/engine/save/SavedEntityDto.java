package com.hsw.simonapp.engine.save;

import java.util.Objects;

final class SavedEntityDto {

    final int entityId;
    final int ownerId;
    final String controlMode;
    final String touchInteraction;
    final int textureSlot;
    final String blendMode;
    final int layer;
    final int renderOrder;
    final SavedScissorDto scissor;
    final SavedTextureRegionDto textureRegion;
    final float x;
    final float y;
    final float z;
    final float scaleX;
    final float scaleY;
    final float rotationDeg;
    final float animationState;
    final float velocityX;
    final float velocityY;
    final float angularVelocityDeg;
    final float animationSpeed;
    final float collisionRadius;

    SavedEntityDto(int entityId,
                   int ownerId,
                   String controlMode,
                   String touchInteraction,
                   int textureSlot,
                   String blendMode,
                   int layer,
                   int renderOrder,
                   SavedScissorDto scissor,
                   SavedTextureRegionDto textureRegion,
                   float x,
                   float y,
                   float z,
                   float scaleX,
                   float scaleY,
                   float rotationDeg,
                   float animationState,
                   float velocityX,
                   float velocityY,
                   float angularVelocityDeg,
                   float animationSpeed,
                   float collisionRadius) {
        this.entityId = entityId;
        this.ownerId = ownerId;
        this.controlMode = Objects.requireNonNull(controlMode, "controlMode");
        this.touchInteraction = Objects.requireNonNull(touchInteraction, "touchInteraction");
        this.textureSlot = textureSlot;
        this.blendMode = Objects.requireNonNull(blendMode, "blendMode");
        this.layer = layer;
        this.renderOrder = renderOrder;
        this.scissor = Objects.requireNonNull(scissor, "scissor");
        this.textureRegion = Objects.requireNonNull(textureRegion, "textureRegion");
        this.x = x;
        this.y = y;
        this.z = z;
        this.scaleX = scaleX;
        this.scaleY = scaleY;
        this.rotationDeg = rotationDeg;
        this.animationState = animationState;
        this.velocityX = velocityX;
        this.velocityY = velocityY;
        this.angularVelocityDeg = angularVelocityDeg;
        this.animationSpeed = animationSpeed;
        this.collisionRadius = collisionRadius;
    }
}
