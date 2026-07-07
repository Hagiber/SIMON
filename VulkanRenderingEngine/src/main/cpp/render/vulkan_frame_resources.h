#pragma once

#include <string>
#include <vector>

#include <vulkan/vulkan.h>

#include "../core/render_scene.h"
#include "vulkan_pipeline.h"
#include "vulkan_types.h"

namespace nativelib::render {

class FrameResources {
public:
    bool createCommandPool(VkDevice device, uint32_t graphicsQueueFamilyIndex, std::string *errorMessage);
    bool createSyncObjects(VkDevice device, std::string *errorMessage);
    bool createFramebuffers(VkDevice device,
                            VkRenderPass renderPass,
                            const std::vector<VkImageView> &imageViews,
                            VkExtent2D extent,
                            std::string *errorMessage);
    bool allocateCommandBuffers(VkDevice device, size_t commandBufferCount, std::string *errorMessage);
    bool recordCommandBuffers(VkDevice device,
                              VkRenderPass renderPass,
                              VkExtent2D extent,
                              const Pipeline &pipeline,
                              VkBuffer vertexBuffer,
                              const std::vector<VkBuffer> &instanceBuffers,
                              const std::vector<VkDescriptorSet> &cameraDescriptorSets,
                              const std::vector<VkDescriptorSet> &textureDescriptorSets,
                              const std::vector<TextureResource> &textures,
                              const std::vector<SpriteDrawBatch> &spriteDrawBatches,
                              std::string *errorMessage);

    VkCommandBuffer beginOneTimeCommands(VkDevice device, std::string *errorMessage) const;
    bool endOneTimeCommands(VkDevice device,
                            VkQueue graphicsQueue,
                            VkCommandBuffer commandBuffer,
                            std::string *errorMessage) const;

    void clearSwapchainResources(VkDevice device);
    void destroy(VkDevice device);

    VkSemaphore imageAvailableSemaphore(uint32_t frameIndex) const;
    VkSemaphore renderFinishedSemaphore(uint32_t frameIndex) const;
    VkFence inFlightFence(uint32_t frameIndex) const;
    VkFence imageInFlightFence(uint32_t imageIndex) const;
    void setImageInFlightFence(uint32_t imageIndex, VkFence fence);
    VkCommandBuffer commandBuffer(uint32_t frameIndex, uint32_t imageIndex) const;
    size_t commandBufferCount() const;
    size_t swapchainImageCount() const;

private:
    VkCommandPool commandPool_ = VK_NULL_HANDLE;
    std::vector<VkCommandBuffer> commandBuffers_;
    std::vector<VkFramebuffer> framebuffers_;
    std::vector<VkSemaphore> imageAvailableSemaphores_;
    std::vector<VkSemaphore> renderFinishedSemaphores_;
    std::vector<VkFence> inFlightFences_;
    std::vector<VkFence> imagesInFlight_;
    size_t swapchainImageCount_ = 0;
};

} // namespace nativelib::render
