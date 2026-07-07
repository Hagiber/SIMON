#pragma once

#include <cstdint>
#include <string>
#include <vector>

#include <vulkan/vulkan.h>

#include "../core/render_scene.h"
#include "vulkan_device.h"
#include "vulkan_types.h"

namespace nativelib::render {

class SpriteBatcher {
public:
    bool create(const VulkanDevice &device, std::string *errorMessage);
    void reset();
    void destroy(VkDevice device);

    uint32_t findNextTextureSlot() const;
    bool setTexture(uint32_t logicalSlot,
                    const uint8_t *pixels,
                    uint32_t width,
                    uint32_t height,
                    std::string *errorMessage);
    bool clearTexture(uint32_t logicalSlot, std::string *errorMessage);
    void clearTextures();

    bool build(const std::vector<nativelib::RenderSprite> &sprites,
               std::string *errorMessage);
    bool uploadInstances(VkDevice device, uint32_t frameIndex, std::string *errorMessage);

    bool hasAtlasTexture() const;
    const uint8_t *atlasPixels() const;
    uint32_t atlasWidth() const;
    uint32_t atlasHeight() const;

    const std::vector<VkBuffer> &instanceBuffers() const;
    const std::vector<SpriteDrawBatch> &batches() const;
    bool commandLayoutDirty() const;
    void clearCommandLayoutDirty();

private:
    struct TextureSource {
        bool occupied = false;
        uint32_t width = 0;
        uint32_t height = 0;
        std::vector<uint8_t> pixels;
    };

    struct AtlasRegion {
        bool occupied = false;
        uint32_t x = 0;
        uint32_t y = 0;
        uint32_t width = 0;
        uint32_t height = 0;
        float u = 0.0f;
        float v = 0.0f;
        float widthUv = 0.0f;
        float heightUv = 0.0f;
    };

    struct PackedRegion {
        uint32_t slot = 0;
        uint32_t x = 0;
        uint32_t y = 0;
        uint32_t packedWidth = 0;
        uint32_t packedHeight = 0;
    };

    bool rebuildAtlas(std::string *errorMessage);
    bool tryPack(uint32_t atlasSize, std::vector<PackedRegion> &packedRegions) const;
    void copyTextureWithPadding(const TextureSource &source, const PackedRegion &packedRegion);
    std::vector<TextureSource> textureSources_;
    std::vector<AtlasRegion> atlasRegions_;
    std::vector<uint8_t> atlasPixels_;
    uint32_t atlasWidth_ = 0;
    uint32_t atlasHeight_ = 0;

    std::vector<SpriteInstance> instances_;
    std::vector<SpriteDrawBatch> batches_;
    bool commandLayoutDirty_ = false;
    uint32_t instanceCapacity_ = 0;
    std::vector<VkBuffer> instanceBuffers_;
    std::vector<VkDeviceMemory> instanceBufferMemories_;
};

} // namespace nativelib::render
