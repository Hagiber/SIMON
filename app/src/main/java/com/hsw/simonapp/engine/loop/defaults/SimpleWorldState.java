package com.hsw.simonapp.engine.loop.defaults;

import com.hsw.simonapp.engine.loop.core.WorldState;
import com.hsw.simonapp.engine.api.BlendMode;
import com.hsw.simonapp.engine.api.ScissorRect;
import com.hsw.simonapp.engine.api.TextureRegion;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class SimpleWorldState implements WorldState {

    private final List<EntityState> entities;

    public SimpleWorldState(List<EntityState> entities) {
        this.entities = new ArrayList<>(entities.size());
        for (EntityState entity : entities) {
            this.entities.add(entity.copy());
        }
    }

    private SimpleWorldState(SimpleWorldState source) {
        this(source.getEntities());
    }

    public SimpleWorldState copy() {
        return new SimpleWorldState(this);
    }

    public List<EntityState> getEntities() {
        return Collections.unmodifiableList(entities);
    }

    public EntityState findEntityById(int entityId) {
        for (EntityState entity : entities) {
            if (entity.getEntityId() == entityId) {
                return entity;
            }
        }
        return null;
    }

    public static final class EntityState {
        public enum ControlMode {
            HUMAN_TOUCH,
            AI
        }

        public enum TouchInteraction {
            NONE,
            SELECT,
            MOVE_TO_TOUCH,
            BUTTON_PRESS
        }

        private final int entityId;
        private final int ownerId;
        private final ControlMode controlMode;
        private final TouchInteraction touchInteraction;
        private final int textureSlot;
        private final BlendMode blendMode;
        private final int layer;
        private final int renderOrder;
        private final ScissorRect scissorRect;
        private final TextureRegion textureRegion;
        private float x;
        private float y;
        private final float z;
        private float scaleX;
        private float scaleY;
        private float rotationDeg;
        private float animationState;
        private float velocityX;
        private float velocityY;
        private final float angularVelocityDeg;
        private final float animationSpeed;
        private final float collisionRadius;

        public EntityState(int entityId,
                           int ownerId,
                           ControlMode controlMode,
                           int textureSlot,
                           float x,
                           float y,
                           float z,
                           float rotationDeg,
                           float velocityX,
                           float velocityY,
                           float angularVelocityDeg,
                           float collisionRadius) {
            this(entityId,
                    ownerId,
                    controlMode,
                    textureSlot,
                    x,
                    y,
                    z,
                    rotationDeg,
                    velocityX,
                    velocityY,
                    angularVelocityDeg,
                    collisionRadius,
                    BlendMode.ALPHA);
        }

        public EntityState(int entityId,
                           int ownerId,
                           ControlMode controlMode,
                           int textureSlot,
                           float x,
                           float y,
                           float z,
                           float rotationDeg,
                           float velocityX,
                           float velocityY,
                           float angularVelocityDeg,
                           float collisionRadius,
                           BlendMode blendMode) {
            this(entityId,
                    ownerId,
                    controlMode,
                    textureSlot,
                    x,
                    y,
                    z,
                    rotationDeg,
                    0.0f,
                    velocityX,
                    velocityY,
                    angularVelocityDeg,
                    0.0f,
                    collisionRadius,
                    blendMode);
        }

        public EntityState(int entityId,
                           int ownerId,
                           ControlMode controlMode,
                           int textureSlot,
                           float x,
                           float y,
                           float z,
                           float rotationDeg,
                           float animationState,
                           float velocityX,
                           float velocityY,
                           float angularVelocityDeg,
                           float animationSpeed,
                           float collisionRadius) {
            this(entityId,
                    ownerId,
                    controlMode,
                    textureSlot,
                    x,
                    y,
                    z,
                    rotationDeg,
                    animationState,
                    velocityX,
                    velocityY,
                    angularVelocityDeg,
                    animationSpeed,
                    collisionRadius,
                    BlendMode.ALPHA);
        }

        public EntityState(int entityId,
                           int ownerId,
                           ControlMode controlMode,
                           int textureSlot,
                           float x,
                           float y,
                           float z,
                           float rotationDeg,
                           float animationState,
                           float velocityX,
                           float velocityY,
                           float angularVelocityDeg,
                           float animationSpeed,
                           float collisionRadius,
                           BlendMode blendMode) {
            this(entityId,
                    ownerId,
                    controlMode,
                    textureSlot,
                    x,
                    y,
                    z,
                    rotationDeg,
                    animationState,
                    velocityX,
                    velocityY,
                    angularVelocityDeg,
                    animationSpeed,
                    collisionRadius,
                    blendMode,
                    0,
                    0,
                    ScissorRect.disabled(),
                    TextureRegion.full(),
                    1.0f,
                    1.0f);
        }

        public EntityState(int entityId,
                           int ownerId,
                           ControlMode controlMode,
                           int textureSlot,
                           float x,
                           float y,
                           float z,
                           float rotationDeg,
                           float animationState,
                           float velocityX,
                           float velocityY,
                           float angularVelocityDeg,
                           float animationSpeed,
                           float collisionRadius,
                           BlendMode blendMode,
                           int layer,
                           int renderOrder,
                           ScissorRect scissorRect) {
            this(entityId,
                    ownerId,
                    controlMode,
                    textureSlot,
                    x,
                    y,
                    z,
                    rotationDeg,
                    animationState,
                    velocityX,
                    velocityY,
                    angularVelocityDeg,
                    animationSpeed,
                    collisionRadius,
                    blendMode,
                    layer,
                    renderOrder,
                    scissorRect,
                    TextureRegion.full());
        }

        public EntityState(int entityId,
                           int ownerId,
                           ControlMode controlMode,
                           int textureSlot,
                           float x,
                           float y,
                           float z,
                           float rotationDeg,
                           float animationState,
                           float velocityX,
                           float velocityY,
                           float angularVelocityDeg,
                           float animationSpeed,
                           float collisionRadius,
                           BlendMode blendMode,
                           int layer,
                           int renderOrder,
                           ScissorRect scissorRect,
                           TextureRegion textureRegion) {
            this(entityId,
                    ownerId,
                    controlMode,
                    textureSlot,
                    x,
                    y,
                    z,
                    rotationDeg,
                    animationState,
                    velocityX,
                    velocityY,
                    angularVelocityDeg,
                    animationSpeed,
                    collisionRadius,
                    blendMode,
                    layer,
                    renderOrder,
                    scissorRect,
                    textureRegion,
                    1.0f,
                    1.0f);
        }

        public EntityState(int entityId,
                           int ownerId,
                           ControlMode controlMode,
                           int textureSlot,
                           float x,
                           float y,
                           float z,
                           float rotationDeg,
                           float animationState,
                           float velocityX,
                           float velocityY,
                           float angularVelocityDeg,
                           float animationSpeed,
                           float collisionRadius,
                           BlendMode blendMode,
                           int layer,
                           int renderOrder,
                           ScissorRect scissorRect,
                           TextureRegion textureRegion,
                           float scaleX,
                           float scaleY) {
            this(entityId,
                    ownerId,
                    controlMode,
                    textureSlot,
                    x,
                    y,
                    z,
                    rotationDeg,
                    animationState,
                    velocityX,
                    velocityY,
                    angularVelocityDeg,
                    animationSpeed,
                    collisionRadius,
                    blendMode,
                    layer,
                    renderOrder,
                    scissorRect,
                    textureRegion,
                    scaleX,
                    scaleY,
                    defaultTouchInteraction(controlMode));
        }

        public EntityState(int entityId,
                           int ownerId,
                           ControlMode controlMode,
                           int textureSlot,
                           float x,
                           float y,
                           float z,
                           float rotationDeg,
                           float animationState,
                           float velocityX,
                           float velocityY,
                           float angularVelocityDeg,
                           float animationSpeed,
                           float collisionRadius,
                           BlendMode blendMode,
                           int layer,
                           int renderOrder,
                           ScissorRect scissorRect,
                           TextureRegion textureRegion,
                           float scaleX,
                           float scaleY,
                           TouchInteraction touchInteraction) {
            this.entityId = entityId;
            this.ownerId = ownerId;
            this.controlMode = Objects.requireNonNull(controlMode, "controlMode");
            this.touchInteraction = Objects.requireNonNull(touchInteraction, "touchInteraction");
            this.textureSlot = textureSlot;
            this.blendMode = Objects.requireNonNull(blendMode, "blendMode");
            this.layer = layer;
            this.renderOrder = renderOrder;
            this.scissorRect = Objects.requireNonNull(scissorRect, "scissorRect");
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

        public static TouchInteraction defaultTouchInteraction(ControlMode controlMode) {
            return Objects.requireNonNull(controlMode, "controlMode") == ControlMode.HUMAN_TOUCH
                    ? TouchInteraction.MOVE_TO_TOUCH
                    : TouchInteraction.SELECT;
        }

        public int getEntityId() {
            return entityId;
        }

        public int getOwnerId() {
            return ownerId;
        }

        public ControlMode getControlMode() {
            return controlMode;
        }

        public TouchInteraction getTouchInteraction() {
            return touchInteraction;
        }

        public int getTextureSlot() {
            return textureSlot;
        }

        public BlendMode getBlendMode() {
            return blendMode;
        }

        public int getLayer() {
            return layer;
        }

        public int getRenderOrder() {
            return renderOrder;
        }

        public ScissorRect getScissorRect() {
            return scissorRect;
        }

        public TextureRegion getTextureRegion() {
            return textureRegion;
        }

        public float getX() {
            return x;
        }

        public void setX(float x) {
            this.x = x;
        }

        public float getY() {
            return y;
        }

        public void setY(float y) {
            this.y = y;
        }

        public float getZ() {
            return z;
        }

        public float getScaleX() {
            return scaleX;
        }

        public void setScaleX(float scaleX) {
            this.scaleX = scaleX;
        }

        public float getScaleY() {
            return scaleY;
        }

        public void setScaleY(float scaleY) {
            this.scaleY = scaleY;
        }

        public float getRotationDeg() {
            return rotationDeg;
        }

        public void setRotationDeg(float rotationDeg) {
            this.rotationDeg = rotationDeg;
        }

        public float getAnimationState() {
            return animationState;
        }

        public void setAnimationState(float animationState) {
            this.animationState = animationState;
        }

        public float getVelocityX() {
            return velocityX;
        }

        public void setVelocityX(float velocityX) {
            this.velocityX = velocityX;
        }

        public float getVelocityY() {
            return velocityY;
        }

        public void setVelocityY(float velocityY) {
            this.velocityY = velocityY;
        }

        public float getAngularVelocityDeg() {
            return angularVelocityDeg;
        }

        public float getAnimationSpeed() {
            return animationSpeed;
        }

        public float getCollisionRadius() {
            return collisionRadius;
        }

        private EntityState copy() {
            return new EntityState(entityId,
                    ownerId,
                    controlMode,
                    textureSlot,
                    x,
                    y,
                    z,
                    rotationDeg,
                    animationState,
                    velocityX,
                    velocityY,
                    angularVelocityDeg,
                    animationSpeed,
                    collisionRadius,
                    blendMode,
                    layer,
                    renderOrder,
                    scissorRect,
                    textureRegion,
                    scaleX,
                    scaleY,
                    touchInteraction);
        }
    }
}


