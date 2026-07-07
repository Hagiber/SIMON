#pragma once

#include <cstddef>
#include <cstdint>

namespace nativelib {

constexpr int32_t kSceneFramePayloadVersion = 1;
constexpr uint32_t kMaxSceneSprites = 4096;
constexpr uint32_t kMaxSceneTextureSlot = 1023;

enum class BlendMode : uint32_t {
    Alpha = 0,
    Additive = 1,
    Multiply = 2
};

constexpr std::size_t kBlendModeCount = 3;

struct OrthoCamera {
    float x = 0.0f;
    float y = 0.0f;
    float zoom = 1.0f;
    float rotationDeg = 0.0f;
};

struct Transform2D {
    float x = 0.0f;
    float y = 0.0f;
    float z = 0.0f;
    float scaleX = 1.0f;
    float scaleY = 1.0f;
    float rotationDeg = 0.0f;
};

struct RenderScissor {
    bool enabled = false;
    int32_t x = 0;
    int32_t y = 0;
    uint32_t width = 0;
    uint32_t height = 0;
};

struct TextureRegion {
    float u = 0.0f;
    float v = 0.0f;
    float widthUv = 1.0f;
    float heightUv = 1.0f;
};

struct RenderSprite {
    uint32_t textureSlot = 0;
    BlendMode blendMode = BlendMode::Alpha;
    int32_t layer = 0;
    int32_t renderOrder = 0;
    RenderScissor scissor;
    TextureRegion textureRegion;
    Transform2D transform;
    float animationState = 0.0f;
};

} // namespace nativelib

