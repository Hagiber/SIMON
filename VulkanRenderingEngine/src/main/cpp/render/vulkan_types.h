#pragma once

#include <cstdint>

#include <vulkan/vulkan.h>

#include "../core/render_scene.h"

namespace nativelib::render {

constexpr uint32_t kInitialTextureCapacity = 4;
constexpr uint32_t kSpriteAtlasTextureSlot = 0;
constexpr uint32_t kMaxFramesInFlight = 2;
constexpr uint32_t kPreferredSwapchainImageCount = kMaxFramesInFlight + 1;

struct Vertex {
    float pos[2];
    float uv[2];
    float color[4];
};

struct TextureResource {
    VkImage image = VK_NULL_HANDLE;
    VkDeviceMemory imageMemory = VK_NULL_HANDLE;
    VkImageView imageView = VK_NULL_HANDLE;
    VkSampler sampler = VK_NULL_HANDLE;
    uint32_t width = 0;
    uint32_t height = 0;
};

struct SpriteInstance {
    float model[8];  // row0: m00, m01, translate x, z; row1: m10, m11, translate y, unused
    float uvRect[4]; // offset u, offset v, scale u, scale v
    float color[4];
};

struct SpriteDrawBatch {
    uint32_t textureSlot = 0;
    nativelib::BlendMode blendMode = nativelib::BlendMode::Alpha;
    nativelib::RenderScissor scissor;
    uint32_t firstInstance = 0;
    uint32_t instanceCount = 0;
};

} // namespace nativelib::render
