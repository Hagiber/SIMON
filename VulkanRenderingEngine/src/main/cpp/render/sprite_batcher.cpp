#include "sprite_batcher.h"

#include <algorithm>
#include <cmath>
#include <cstring>
#include <limits>
#include <utility>

namespace nativelib::render {
namespace {

constexpr uint32_t kAtlasPaddingPixels = 1;
constexpr uint32_t kMinimumAtlasSize = 64;
constexpr uint32_t kMaximumAtlasSize = 4096;

bool fail(std::string *errorMessage, const char *message) {
    if (errorMessage != nullptr) {
        *errorMessage = message;
    }
    return false;
}

uint32_t nextPowerOfTwo(uint32_t value) {
    if (value <= 1) {
        return 1;
    }

    --value;
    value |= value >> 1u;
    value |= value >> 2u;
    value |= value >> 4u;
    value |= value >> 8u;
    value |= value >> 16u;
    return value + 1;
}

uint32_t clampToRange(int value, uint32_t upperExclusive) {
    if (value < 0) {
        return 0;
    }
    uint32_t unsignedValue = static_cast<uint32_t>(value);
    if (unsignedValue >= upperExclusive) {
        return upperExclusive - 1;
    }
    return unsignedValue;
}

uint8_t premultiplyColor(uint8_t color, uint8_t alpha) {
    return static_cast<uint8_t>((static_cast<uint32_t>(color) * alpha + 127u) / 255u);
}

bool sameScissor(const nativelib::RenderScissor &a, const nativelib::RenderScissor &b) {
    if (!a.enabled && !b.enabled) {
        return true;
    }
    return a.enabled == b.enabled &&
           a.x == b.x &&
           a.y == b.y &&
           a.width == b.width &&
           a.height == b.height;
}

bool sameBatchLayout(const std::vector<SpriteDrawBatch> &a,
                     const std::vector<SpriteDrawBatch> &b) {
    if (a.size() != b.size()) {
        return false;
    }

    for (size_t i = 0; i < a.size(); ++i) {
        if (a[i].textureSlot != b[i].textureSlot ||
            a[i].blendMode != b[i].blendMode ||
            a[i].firstInstance != b[i].firstInstance ||
            a[i].instanceCount != b[i].instanceCount ||
            !sameScissor(a[i].scissor, b[i].scissor)) {
            return false;
        }
    }
    return true;
}

void textureAspectScale(uint32_t regionWidth,
                        uint32_t regionHeight,
                        const nativelib::TextureRegion &textureRegion,
                        float &scaleX,
                        float &scaleY) {
    scaleX = 1.0f;
    scaleY = 1.0f;

    const float visibleWidth = static_cast<float>(regionWidth) * textureRegion.widthUv;
    const float visibleHeight = static_cast<float>(regionHeight) * textureRegion.heightUv;
    if (visibleWidth <= 0.0f || visibleHeight <= 0.0f) {
        return;
    }

    if (visibleWidth > visibleHeight) {
        scaleY = visibleHeight / visibleWidth;
    } else if (visibleHeight > visibleWidth) {
        scaleX = visibleWidth / visibleHeight;
    }
}

} // namespace

bool SpriteBatcher::create(const VulkanDevice &device, std::string *errorMessage) {
    instanceCapacity_ = nativelib::kMaxSceneSprites;
    instanceBuffers_.resize(kMaxFramesInFlight, VK_NULL_HANDLE);
    instanceBufferMemories_.resize(kMaxFramesInFlight, VK_NULL_HANDLE);
    const VkDeviceSize bufferSize =
            static_cast<VkDeviceSize>(instanceCapacity_) * sizeof(SpriteInstance);

    for (uint32_t frameIndex = 0; frameIndex < kMaxFramesInFlight; ++frameIndex) {
        if (!device.createBuffer(bufferSize,
                                 VK_BUFFER_USAGE_VERTEX_BUFFER_BIT,
                                 VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT |
                                         VK_MEMORY_PROPERTY_HOST_COHERENT_BIT,
                                 instanceBuffers_[frameIndex],
                                 instanceBufferMemories_[frameIndex],
                                 errorMessage)) {
            return false;
        }
    }
    return true;
}

void SpriteBatcher::reset() {
    textureSources_.clear();
    atlasRegions_.clear();
    atlasPixels_.clear();
    atlasWidth_ = 0;
    atlasHeight_ = 0;
    instances_.clear();
    batches_.clear();
    commandLayoutDirty_ = false;
}

void SpriteBatcher::destroy(VkDevice device) {
    if (device != VK_NULL_HANDLE) {
        for (VkBuffer instanceBuffer : instanceBuffers_) {
            if (instanceBuffer != VK_NULL_HANDLE) {
                vkDestroyBuffer(device, instanceBuffer, nullptr);
            }
        }
        for (VkDeviceMemory instanceBufferMemory : instanceBufferMemories_) {
            if (instanceBufferMemory != VK_NULL_HANDLE) {
                vkFreeMemory(device, instanceBufferMemory, nullptr);
            }
        }
    }

    instanceBuffers_.clear();
    instanceBufferMemories_.clear();
    instanceCapacity_ = 0;
    reset();
}

uint32_t SpriteBatcher::findNextTextureSlot() const {
    for (uint32_t slot = 0; slot < textureSources_.size(); ++slot) {
        if (!textureSources_[slot].occupied) {
            return slot;
        }
    }
    return static_cast<uint32_t>(textureSources_.size());
}

bool SpriteBatcher::setTexture(uint32_t logicalSlot,
                               const uint8_t *pixels,
                               uint32_t width,
                               uint32_t height,
                               std::string *errorMessage) {
    if (pixels == nullptr || width == 0 || height == 0) {
        return fail(errorMessage, "Invalid sprite texture input data");
    }
    if (width > kMaximumAtlasSize - kAtlasPaddingPixels * 2 ||
        height > kMaximumAtlasSize - kAtlasPaddingPixels * 2) {
        return fail(errorMessage, "Sprite texture is larger than the atlas limit");
    }

    uint64_t pixelCount = static_cast<uint64_t>(width) * static_cast<uint64_t>(height);
    if (pixelCount > std::numeric_limits<size_t>::max() / 4u) {
        return fail(errorMessage, "Sprite texture is too large");
    }

    if (logicalSlot >= textureSources_.size()) {
        textureSources_.resize(static_cast<size_t>(logicalSlot) + 1);
        atlasRegions_.resize(textureSources_.size());
    }

    TextureSource &source = textureSources_[logicalSlot];
    const size_t byteCount = static_cast<size_t>(pixelCount * 4u);
    std::vector<uint8_t> premultipliedPixels(byteCount);
    for (size_t offset = 0; offset < byteCount; offset += 4u) {
        uint8_t alpha = pixels[offset + 3u];
        premultipliedPixels[offset + 0u] = premultiplyColor(pixels[offset + 0u], alpha);
        premultipliedPixels[offset + 1u] = premultiplyColor(pixels[offset + 1u], alpha);
        premultipliedPixels[offset + 2u] = premultiplyColor(pixels[offset + 2u], alpha);
        premultipliedPixels[offset + 3u] = alpha;
    }

    if (source.occupied &&
        source.width == width &&
        source.height == height &&
        source.pixels == premultipliedPixels) {
        return true;
    }

    source.occupied = true;
    source.width = width;
    source.height = height;
    source.pixels = std::move(premultipliedPixels);

    return rebuildAtlas(errorMessage);
}

bool SpriteBatcher::clearTexture(uint32_t logicalSlot, std::string *errorMessage) {
    if (logicalSlot >= textureSources_.size() || !textureSources_[logicalSlot].occupied) {
        return true;
    }

    textureSources_[logicalSlot] = TextureSource{};
    if (logicalSlot < atlasRegions_.size()) {
        atlasRegions_[logicalSlot] = AtlasRegion{};
    }
    return rebuildAtlas(errorMessage);
}

void SpriteBatcher::clearTextures() {
    textureSources_.clear();
    atlasRegions_.clear();
    atlasPixels_.clear();
    atlasWidth_ = 0;
    atlasHeight_ = 0;
}

bool SpriteBatcher::build(const std::vector<nativelib::RenderSprite> &sprites,
                          std::string *errorMessage) {
    if (sprites.size() > instanceCapacity_) {
        return fail(errorMessage, "Scene exceeds sprite instance buffer capacity");
    }

    struct OrderedInstance {
        uint32_t textureSlot = kSpriteAtlasTextureSlot;
        nativelib::BlendMode blendMode = nativelib::BlendMode::Alpha;
        int32_t layer = 0;
        int32_t renderOrder = 0;
        float z = 0.0f;
        uint32_t submissionIndex = 0;
        nativelib::RenderScissor scissor;
        SpriteInstance instance{};
    };

    std::vector<SpriteInstance> nextInstances;
    std::vector<SpriteDrawBatch> nextBatches;

    std::vector<OrderedInstance> orderedInstances;
    orderedInstances.reserve(sprites.size());
    if (hasAtlasTexture()) {
        uint32_t submissionIndex = 0;
        for (const nativelib::RenderSprite &sprite : sprites) {
            if (sprite.textureSlot >= atlasRegions_.size()) {
                ++submissionIndex;
                continue;
            }
            const AtlasRegion &region = atlasRegions_[sprite.textureSlot];
            if (!region.occupied) {
                ++submissionIndex;
                continue;
            }

            const nativelib::Transform2D &transform = sprite.transform;
            float angle = transform.rotationDeg * 0.0174532925199432957f;
            float c = std::cos(angle);
            float s = std::sin(angle);
            float textureScaleX = 1.0f;
            float textureScaleY = 1.0f;
            textureAspectScale(region.width,
                               region.height,
                               sprite.textureRegion,
                               textureScaleX,
                               textureScaleY);
            const float finalScaleX = transform.scaleX * textureScaleX;
            const float finalScaleY = transform.scaleY * textureScaleY;

            OrderedInstance ordered{};
            ordered.textureSlot = kSpriteAtlasTextureSlot;
            ordered.blendMode = sprite.blendMode;
            ordered.layer = sprite.layer;
            ordered.renderOrder = sprite.renderOrder;
            ordered.z = transform.z;
            ordered.submissionIndex = submissionIndex;
            ordered.scissor = sprite.scissor;
            ordered.instance.model[0] = c * finalScaleX;
            ordered.instance.model[1] = -s * finalScaleY;
            ordered.instance.model[2] = transform.x;
            ordered.instance.model[3] = transform.z;
            ordered.instance.model[4] = s * finalScaleX;
            ordered.instance.model[5] = c * finalScaleY;
            ordered.instance.model[6] = transform.y;
            ordered.instance.model[7] = 0.0f;
            ordered.instance.uvRect[0] = region.u + sprite.textureRegion.u * region.widthUv;
            ordered.instance.uvRect[1] = region.v + sprite.textureRegion.v * region.heightUv;
            ordered.instance.uvRect[2] = sprite.textureRegion.widthUv * region.widthUv;
            ordered.instance.uvRect[3] = sprite.textureRegion.heightUv * region.heightUv;
            ordered.instance.color[0] = 1.0f;
            ordered.instance.color[1] = 1.0f;
            ordered.instance.color[2] = 1.0f;
            ordered.instance.color[3] = 1.0f;
            orderedInstances.push_back(ordered);
            ++submissionIndex;
        }
    }

    std::sort(orderedInstances.begin(),
              orderedInstances.end(),
              [](const OrderedInstance &a, const OrderedInstance &b) {
                  if (a.layer != b.layer) {
                      return a.layer < b.layer;
                  }
                  if (a.renderOrder != b.renderOrder) {
                      return a.renderOrder < b.renderOrder;
                  }
                  if (a.z != b.z) {
                      return a.z < b.z;
                  }
                  return a.submissionIndex < b.submissionIndex;
              });

    nextInstances.reserve(orderedInstances.size());
    for (const OrderedInstance &ordered : orderedInstances) {
        uint32_t instanceIndex = static_cast<uint32_t>(nextInstances.size());
        if (nextBatches.empty() ||
            nextBatches.back().textureSlot != ordered.textureSlot ||
            nextBatches.back().blendMode != ordered.blendMode ||
            !sameScissor(nextBatches.back().scissor, ordered.scissor) ||
            nextBatches.back().firstInstance + nextBatches.back().instanceCount != instanceIndex) {
            SpriteDrawBatch batch{};
            batch.textureSlot = ordered.textureSlot;
            batch.blendMode = ordered.blendMode;
            batch.scissor = ordered.scissor;
            batch.firstInstance = instanceIndex;
            batch.instanceCount = 1;
            nextBatches.push_back(batch);
        } else {
            nextBatches.back().instanceCount += 1;
        }
        nextInstances.push_back(ordered.instance);
    }

    bool layoutDirty = !sameBatchLayout(batches_, nextBatches);
    instances_ = std::move(nextInstances);
    batches_ = std::move(nextBatches);

    commandLayoutDirty_ = commandLayoutDirty_ || layoutDirty;
    return true;
}

bool SpriteBatcher::hasAtlasTexture() const {
    return !atlasPixels_.empty() && atlasWidth_ > 0 && atlasHeight_ > 0;
}

const uint8_t *SpriteBatcher::atlasPixels() const {
    return atlasPixels_.empty() ? nullptr : atlasPixels_.data();
}

uint32_t SpriteBatcher::atlasWidth() const {
    return atlasWidth_;
}

uint32_t SpriteBatcher::atlasHeight() const {
    return atlasHeight_;
}

const std::vector<VkBuffer> &SpriteBatcher::instanceBuffers() const {
    return instanceBuffers_;
}

const std::vector<SpriteDrawBatch> &SpriteBatcher::batches() const {
    return batches_;
}

bool SpriteBatcher::commandLayoutDirty() const {
    return commandLayoutDirty_;
}

void SpriteBatcher::clearCommandLayoutDirty() {
    commandLayoutDirty_ = false;
}

bool SpriteBatcher::rebuildAtlas(std::string *errorMessage) {
    uint32_t maxSourceDimension = 0;
    uint64_t paddedArea = 0;
    bool hasTexture = false;

    for (const TextureSource &source : textureSources_) {
        if (!source.occupied) {
            continue;
        }
        hasTexture = true;
        maxSourceDimension = std::max(maxSourceDimension,
                                      std::max(source.width, source.height) + kAtlasPaddingPixels * 2u);
        paddedArea += static_cast<uint64_t>(source.width + kAtlasPaddingPixels * 2u) *
                      static_cast<uint64_t>(source.height + kAtlasPaddingPixels * 2u);
    }

    if (!hasTexture) {
        atlasPixels_.clear();
        atlasWidth_ = 0;
        atlasHeight_ = 0;
        std::fill(atlasRegions_.begin(), atlasRegions_.end(), AtlasRegion{});
        return true;
    }

    uint32_t atlasSize = nextPowerOfTwo(std::max(kMinimumAtlasSize, maxSourceDimension));
    while (static_cast<uint64_t>(atlasSize) * static_cast<uint64_t>(atlasSize) < paddedArea &&
           atlasSize < kMaximumAtlasSize) {
        atlasSize *= 2u;
    }

    std::vector<PackedRegion> packedRegions;
    bool packed = false;
    while (atlasSize <= kMaximumAtlasSize) {
        packedRegions.clear();
        if (tryPack(atlasSize, packedRegions)) {
            packed = true;
            break;
        }
        atlasSize *= 2u;
    }

    if (!packed) {
        return fail(errorMessage, "Sprite atlas does not fit within the atlas limit");
    }

    atlasWidth_ = atlasSize;
    atlasHeight_ = atlasSize;
    atlasPixels_.assign(static_cast<size_t>(atlasWidth_) * static_cast<size_t>(atlasHeight_) * 4u, 0);
    std::fill(atlasRegions_.begin(), atlasRegions_.end(), AtlasRegion{});

    for (const PackedRegion &packedRegion : packedRegions) {
        const TextureSource &source = textureSources_[packedRegion.slot];
        copyTextureWithPadding(source, packedRegion);

        AtlasRegion &region = atlasRegions_[packedRegion.slot];
        region.occupied = true;
        region.x = packedRegion.x + kAtlasPaddingPixels;
        region.y = packedRegion.y + kAtlasPaddingPixels;
        region.width = source.width;
        region.height = source.height;
        region.u = static_cast<float>(region.x) / static_cast<float>(atlasWidth_);
        region.v = static_cast<float>(region.y) / static_cast<float>(atlasHeight_);
        region.widthUv = static_cast<float>(region.width) / static_cast<float>(atlasWidth_);
        region.heightUv = static_cast<float>(region.height) / static_cast<float>(atlasHeight_);
    }

    return true;
}

bool SpriteBatcher::tryPack(uint32_t atlasSize, std::vector<PackedRegion> &packedRegions) const {
    std::vector<uint32_t> occupiedSlots;
    occupiedSlots.reserve(textureSources_.size());
    for (uint32_t slot = 0; slot < textureSources_.size(); ++slot) {
        if (textureSources_[slot].occupied) {
            occupiedSlots.push_back(slot);
        }
    }
    std::sort(occupiedSlots.begin(),
              occupiedSlots.end(),
              [this](uint32_t a, uint32_t b) {
                  const TextureSource &left = textureSources_[a];
                  const TextureSource &right = textureSources_[b];
                  uint64_t leftArea = static_cast<uint64_t>(left.width) * left.height;
                  uint64_t rightArea = static_cast<uint64_t>(right.width) * right.height;
                  if (leftArea != rightArea) {
                      return leftArea > rightArea;
                  }
                  if (left.height != right.height) {
                      return left.height > right.height;
                  }
                  if (left.width != right.width) {
                      return left.width > right.width;
                  }
                  return a < b;
              });

    uint32_t cursorX = 0;
    uint32_t cursorY = 0;
    uint32_t rowHeight = 0;

    for (uint32_t slot : occupiedSlots) {
        const TextureSource &source = textureSources_[slot];
        uint32_t packedWidth = source.width + kAtlasPaddingPixels * 2u;
        uint32_t packedHeight = source.height + kAtlasPaddingPixels * 2u;
        if (packedWidth > atlasSize || packedHeight > atlasSize) {
            return false;
        }

        if (cursorX + packedWidth > atlasSize) {
            cursorX = 0;
            cursorY += rowHeight;
            rowHeight = 0;
        }

        if (cursorY + packedHeight > atlasSize) {
            return false;
        }

        PackedRegion packedRegion{};
        packedRegion.slot = slot;
        packedRegion.x = cursorX;
        packedRegion.y = cursorY;
        packedRegion.packedWidth = packedWidth;
        packedRegion.packedHeight = packedHeight;
        packedRegions.push_back(packedRegion);

        cursorX += packedWidth;
        rowHeight = std::max(rowHeight, packedHeight);
    }

    return true;
}

void SpriteBatcher::copyTextureWithPadding(const TextureSource &source, const PackedRegion &packedRegion) {
    for (uint32_t y = 0; y < packedRegion.packedHeight; ++y) {
        int sourceY = static_cast<int>(y) - static_cast<int>(kAtlasPaddingPixels);
        uint32_t clampedY = clampToRange(sourceY, source.height);
        for (uint32_t x = 0; x < packedRegion.packedWidth; ++x) {
            int sourceX = static_cast<int>(x) - static_cast<int>(kAtlasPaddingPixels);
            uint32_t clampedX = clampToRange(sourceX, source.width);

            size_t sourceIndex = (static_cast<size_t>(clampedY) * source.width + clampedX) * 4u;
            size_t atlasIndex = (static_cast<size_t>(packedRegion.y + y) * atlasWidth_ +
                                 static_cast<size_t>(packedRegion.x + x)) * 4u;
            atlasPixels_[atlasIndex + 0] = source.pixels[sourceIndex + 0];
            atlasPixels_[atlasIndex + 1] = source.pixels[sourceIndex + 1];
            atlasPixels_[atlasIndex + 2] = source.pixels[sourceIndex + 2];
            atlasPixels_[atlasIndex + 3] = source.pixels[sourceIndex + 3];
        }
    }
}

bool SpriteBatcher::uploadInstances(VkDevice device,
                                    uint32_t frameIndex,
                                    std::string *errorMessage) {
    if (instances_.empty()) {
        return true;
    }
    if (frameIndex >= instanceBufferMemories_.size() ||
        instanceBufferMemories_[frameIndex] == VK_NULL_HANDLE) {
        return fail(errorMessage, "Sprite instance buffer is not allocated");
    }

    VkDeviceSize uploadSize = static_cast<VkDeviceSize>(instances_.size()) * sizeof(SpriteInstance);
    void *mapped = nullptr;
    if (vkMapMemory(device,
                    instanceBufferMemories_[frameIndex],
                    0,
                    uploadSize,
                    0,
                    &mapped) != VK_SUCCESS) {
        return fail(errorMessage, "Failed to map sprite instance buffer memory");
    }
    std::memcpy(mapped, instances_.data(), static_cast<size_t>(uploadSize));
    vkUnmapMemory(device, instanceBufferMemories_[frameIndex]);
    return true;
}

} // namespace nativelib::render
