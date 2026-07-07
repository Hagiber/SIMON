#include "vulkan_descriptor_resources.h"

#include <algorithm>
#include <array>
#include <cmath>
#include <cstring>

namespace nativelib::render {
namespace {

bool fail(std::string *errorMessage, const char *message) {
    if (errorMessage != nullptr) {
        *errorMessage = message;
    }
    return false;
}

constexpr float kDegreesToRadians = 0.0174532925199432957f;

} // namespace

bool DescriptorResources::create(const VulkanDevice &device,
                                 std::vector<TextureResource> &textures,
                                 std::string *errorMessage) {
    VkDescriptorSetLayoutBinding cameraBinding{};
    cameraBinding.binding = 0;
    cameraBinding.descriptorType = VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER;
    cameraBinding.descriptorCount = 1;
    cameraBinding.stageFlags = VK_SHADER_STAGE_VERTEX_BIT;

    VkDescriptorSetLayoutCreateInfo cameraLayoutInfo{VK_STRUCTURE_TYPE_DESCRIPTOR_SET_LAYOUT_CREATE_INFO};
    cameraLayoutInfo.bindingCount = 1;
    cameraLayoutInfo.pBindings = &cameraBinding;

    if (vkCreateDescriptorSetLayout(device.device(),
                                    &cameraLayoutInfo,
                                    nullptr,
                                    &cameraDescriptorSetLayout_) != VK_SUCCESS) {
        return fail(errorMessage, "Failed to create camera descriptor set layout");
    }

    VkDescriptorSetLayoutBinding textureBinding{};
    textureBinding.binding = 0;
    textureBinding.descriptorType = VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER;
    textureBinding.descriptorCount = 1;
    textureBinding.stageFlags = VK_SHADER_STAGE_FRAGMENT_BIT;

    VkDescriptorSetLayoutCreateInfo textureLayoutInfo{VK_STRUCTURE_TYPE_DESCRIPTOR_SET_LAYOUT_CREATE_INFO};
    textureLayoutInfo.bindingCount = 1;
    textureLayoutInfo.pBindings = &textureBinding;

    if (vkCreateDescriptorSetLayout(device.device(),
                                    &textureLayoutInfo,
                                    nullptr,
                                    &textureDescriptorSetLayout_) != VK_SUCCESS) {
        return fail(errorMessage, "Failed to create texture descriptor set layout");
    }

    cameraBuffers_.resize(kMaxFramesInFlight, VK_NULL_HANDLE);
    cameraBufferMemories_.resize(kMaxFramesInFlight, VK_NULL_HANDLE);
    const VkDeviceSize cameraBufferSize = sizeof(float) * 16;
    for (uint32_t frameIndex = 0; frameIndex < kMaxFramesInFlight; ++frameIndex) {
        if (!device.createBuffer(cameraBufferSize,
                                 VK_BUFFER_USAGE_UNIFORM_BUFFER_BIT,
                                 VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT |
                                         VK_MEMORY_PROPERTY_HOST_COHERENT_BIT,
                                 cameraBuffers_[frameIndex],
                                 cameraBufferMemories_[frameIndex],
                                 errorMessage)) {
            return false;
        }
    }

    return ensureTextureSlotCapacity(device, textures, kInitialTextureCapacity, errorMessage);
}

void DescriptorResources::destroy(VkDevice device) {
    if (device == VK_NULL_HANDLE) {
        return;
    }

    textureDescriptorSets_.clear();
    cameraDescriptorSets_.clear();

    for (VkBuffer cameraBuffer : cameraBuffers_) {
        if (cameraBuffer != VK_NULL_HANDLE) {
            vkDestroyBuffer(device, cameraBuffer, nullptr);
        }
    }
    cameraBuffers_.clear();
    for (VkDeviceMemory cameraBufferMemory : cameraBufferMemories_) {
        if (cameraBufferMemory != VK_NULL_HANDLE) {
            vkFreeMemory(device, cameraBufferMemory, nullptr);
        }
    }
    cameraBufferMemories_.clear();

    if (descriptorPool_ != VK_NULL_HANDLE) {
        vkDestroyDescriptorPool(device, descriptorPool_, nullptr);
        descriptorPool_ = VK_NULL_HANDLE;
    }
    if (cameraDescriptorSetLayout_ != VK_NULL_HANDLE) {
        vkDestroyDescriptorSetLayout(device, cameraDescriptorSetLayout_, nullptr);
        cameraDescriptorSetLayout_ = VK_NULL_HANDLE;
    }
    if (textureDescriptorSetLayout_ != VK_NULL_HANDLE) {
        vkDestroyDescriptorSetLayout(device, textureDescriptorSetLayout_, nullptr);
        textureDescriptorSetLayout_ = VK_NULL_HANDLE;
    }
}

bool DescriptorResources::ensureTextureSlotCapacity(const VulkanDevice &device,
                                                    std::vector<TextureResource> &textures,
                                                    uint32_t minCount,
                                                    std::string *errorMessage) {
    if (minCount <= textures.size()) {
        return true;
    }

    uint32_t newCapacity = textures.empty() ? kInitialTextureCapacity
                                            : static_cast<uint32_t>(textures.size());
    while (newCapacity < minCount) {
        newCapacity *= 2;
    }

    textures.resize(newCapacity);
    return recreateDescriptorPool(device.device(), textures, newCapacity, errorMessage);
}

uint32_t DescriptorResources::findNextTextureSlot(const std::vector<TextureResource> &textures) const {
    for (uint32_t slot = 0; slot < textures.size(); ++slot) {
        const TextureResource &texture = textures[slot];
        if (texture.image == VK_NULL_HANDLE &&
            texture.imageMemory == VK_NULL_HANDLE &&
            texture.imageView == VK_NULL_HANDLE &&
            texture.sampler == VK_NULL_HANDLE) {
            return slot;
        }
    }
    return static_cast<uint32_t>(textures.size());
}

void DescriptorResources::updateTextureDescriptor(VkDevice device,
                                                  const std::vector<TextureResource> &textures,
                                                  uint32_t slot) {
    if (slot >= textures.size() || slot >= textureDescriptorSets_.size()) {
        return;
    }
    if (textures[slot].imageView == VK_NULL_HANDLE || textures[slot].sampler == VK_NULL_HANDLE) {
        return;
    }

    VkDescriptorImageInfo imageInfo{};
    imageInfo.imageLayout = VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL;
    imageInfo.imageView = textures[slot].imageView;
    imageInfo.sampler = textures[slot].sampler;

    VkWriteDescriptorSet write{VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET};
    write.dstSet = textureDescriptorSets_[slot];
    write.dstBinding = 0;
    write.descriptorCount = 1;
    write.descriptorType = VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER;
    write.pImageInfo = &imageInfo;
    vkUpdateDescriptorSets(device, 1, &write, 0, nullptr);
}

bool DescriptorResources::updateCamera(VkDevice device,
                                       uint32_t frameIndex,
                                       VkExtent2D extent,
                                       const nativelib::OrthoCamera &camera,
                                       std::string *errorMessage) {
    if (frameIndex >= cameraBufferMemories_.size() ||
        cameraBufferMemories_[frameIndex] == VK_NULL_HANDLE) {
        return false;
    }
    if (extent.width == 0 || extent.height == 0) {
        return false;
    }
    if (!std::isfinite(camera.x) || !std::isfinite(camera.y) ||
        !std::isfinite(camera.zoom) || !std::isfinite(camera.rotationDeg) ||
        camera.zoom <= 0.0f) {
        return fail(errorMessage, "Invalid ortho camera");
    }

    const float width = static_cast<float>(extent.width);
    const float height = static_cast<float>(extent.height);
    const float aspect = width / height;
    const float scaleX = aspect >= 1.0f ? (1.0f / aspect) : 1.0f;
    const float scaleY = aspect >= 1.0f ? 1.0f : aspect;
    const float cameraAngle = -camera.rotationDeg * kDegreesToRadians;
    const float c = std::cos(cameraAngle);
    const float s = std::sin(cameraAngle);
    const float zoomedScaleX = scaleX * camera.zoom;
    const float zoomedScaleY = scaleY * camera.zoom;

    const float orthoViewProj[16] = {
            zoomedScaleX * c,
            zoomedScaleY * s,
            0.0f,
            0.0f,
            -zoomedScaleX * s,
            zoomedScaleY * c,
            0.0f,
            0.0f,
            0.0f,
            0.0f,
            1.0f,
            0.0f,
            zoomedScaleX * (-c * camera.x + s * camera.y),
            zoomedScaleY * (-s * camera.x - c * camera.y),
            0.0f,
            1.0f
    };

    void *mapped = nullptr;
    if (vkMapMemory(device,
                    cameraBufferMemories_[frameIndex],
                    0,
                    sizeof(orthoViewProj),
                    0,
                    &mapped) != VK_SUCCESS) {
        return fail(errorMessage, "Failed to map camera buffer memory");
    }
    std::memcpy(mapped, orthoViewProj, sizeof(orthoViewProj));
    vkUnmapMemory(device, cameraBufferMemories_[frameIndex]);
    return true;
}

bool DescriptorResources::recreateDescriptorPool(VkDevice device,
                                                 const std::vector<TextureResource> &textures,
                                                 uint32_t textureCapacity,
                                                 std::string *errorMessage) {
    if (textureCapacity == 0) {
        textureCapacity = 1;
    }

    if (descriptorPool_ != VK_NULL_HANDLE) {
        vkDestroyDescriptorPool(device, descriptorPool_, nullptr);
        descriptorPool_ = VK_NULL_HANDLE;
    }
    cameraDescriptorSets_.clear();
    textureDescriptorSets_.clear();

    std::array<VkDescriptorPoolSize, 2> poolSizes{};
    poolSizes[0].type = VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER;
    poolSizes[0].descriptorCount = kMaxFramesInFlight;
    poolSizes[1].type = VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER;
    poolSizes[1].descriptorCount = textureCapacity;

    VkDescriptorPoolCreateInfo poolInfo{VK_STRUCTURE_TYPE_DESCRIPTOR_POOL_CREATE_INFO};
    poolInfo.poolSizeCount = static_cast<uint32_t>(poolSizes.size());
    poolInfo.pPoolSizes = poolSizes.data();
    poolInfo.maxSets = kMaxFramesInFlight + textureCapacity;

    if (vkCreateDescriptorPool(device, &poolInfo, nullptr, &descriptorPool_) != VK_SUCCESS) {
        return fail(errorMessage, "Failed to create descriptor pool");
    }

    VkDescriptorSetAllocateInfo allocInfo{VK_STRUCTURE_TYPE_DESCRIPTOR_SET_ALLOCATE_INFO};
    allocInfo.descriptorPool = descriptorPool_;
    std::vector<VkDescriptorSetLayout> cameraLayouts(kMaxFramesInFlight,
                                                     cameraDescriptorSetLayout_);
    cameraDescriptorSets_.resize(kMaxFramesInFlight, VK_NULL_HANDLE);
    allocInfo.descriptorSetCount = kMaxFramesInFlight;
    allocInfo.pSetLayouts = cameraLayouts.data();
    if (vkAllocateDescriptorSets(device, &allocInfo, cameraDescriptorSets_.data()) != VK_SUCCESS) {
        return fail(errorMessage, "Failed to allocate camera descriptor set");
    }

    textureDescriptorSets_.resize(textureCapacity, VK_NULL_HANDLE);
    allocInfo.descriptorSetCount = 1;
    allocInfo.pSetLayouts = &textureDescriptorSetLayout_;
    for (uint32_t slot = 0; slot < textureCapacity; ++slot) {
        if (vkAllocateDescriptorSets(device, &allocInfo, &textureDescriptorSets_[slot]) != VK_SUCCESS) {
            return fail(errorMessage, "Failed to allocate texture descriptor set");
        }
    }

    for (uint32_t frameIndex = 0; frameIndex < kMaxFramesInFlight; ++frameIndex) {
        VkDescriptorBufferInfo bufferInfo{};
        bufferInfo.buffer = cameraBuffers_[frameIndex];
        bufferInfo.offset = 0;
        bufferInfo.range = sizeof(float) * 16;

        VkWriteDescriptorSet write{VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET};
        write.dstSet = cameraDescriptorSets_[frameIndex];
        write.dstBinding = 0;
        write.descriptorCount = 1;
        write.descriptorType = VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER;
        write.pBufferInfo = &bufferInfo;
        vkUpdateDescriptorSets(device, 1, &write, 0, nullptr);
    }

    for (uint32_t slot = 0; slot < textureCapacity; ++slot) {
        updateTextureDescriptor(device, textures, slot);
    }

    return true;
}

VkDescriptorSetLayout DescriptorResources::cameraDescriptorSetLayout() const {
    return cameraDescriptorSetLayout_;
}

VkDescriptorSetLayout DescriptorResources::textureDescriptorSetLayout() const {
    return textureDescriptorSetLayout_;
}

const std::vector<VkDescriptorSet> &DescriptorResources::cameraDescriptorSets() const {
    return cameraDescriptorSets_;
}

const std::vector<VkDescriptorSet> &DescriptorResources::textureDescriptorSets() const {
    return textureDescriptorSets_;
}

} // namespace nativelib::render
