#pragma once

#include <string>
#include <vector>

#include <vulkan/vulkan.h>

#include "../core/render_scene.h"
#include "vulkan_device.h"
#include "vulkan_types.h"

namespace nativelib::render {

class DescriptorResources {
public:
    bool create(const VulkanDevice &device,
                std::vector<TextureResource> &textures,
                std::string *errorMessage);
    void destroy(VkDevice device);

    bool ensureTextureSlotCapacity(const VulkanDevice &device,
                                   std::vector<TextureResource> &textures,
                                   uint32_t minCount,
                                   std::string *errorMessage);
    uint32_t findNextTextureSlot(const std::vector<TextureResource> &textures) const;
    void updateTextureDescriptor(VkDevice device,
                                 const std::vector<TextureResource> &textures,
                                 uint32_t slot);
    bool updateCamera(VkDevice device,
                      uint32_t frameIndex,
                      VkExtent2D extent,
                      const nativelib::OrthoCamera &camera,
                      std::string *errorMessage);

    VkDescriptorSetLayout cameraDescriptorSetLayout() const;
    VkDescriptorSetLayout textureDescriptorSetLayout() const;
    const std::vector<VkDescriptorSet> &cameraDescriptorSets() const;
    const std::vector<VkDescriptorSet> &textureDescriptorSets() const;

private:
    bool recreateDescriptorPool(VkDevice device,
                                const std::vector<TextureResource> &textures,
                                uint32_t textureCapacity,
                                std::string *errorMessage);

    VkDescriptorSetLayout cameraDescriptorSetLayout_ = VK_NULL_HANDLE;
    VkDescriptorSetLayout textureDescriptorSetLayout_ = VK_NULL_HANDLE;
    VkDescriptorPool descriptorPool_ = VK_NULL_HANDLE;
    std::vector<VkDescriptorSet> cameraDescriptorSets_;
    std::vector<VkDescriptorSet> textureDescriptorSets_;
    std::vector<VkBuffer> cameraBuffers_;
    std::vector<VkDeviceMemory> cameraBufferMemories_;
};

} // namespace nativelib::render
