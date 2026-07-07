#pragma once

#include <cstdint>
#include <string>
#include <vector>

#include <vulkan/vulkan.h>

namespace nativelib::render {

class VulkanSwapchain {
public:
    bool create(VkPhysicalDevice physicalDevice,
                VkDevice device,
                VkSurfaceKHR surface,
                VkExtent2D fallbackExtent,
                uint32_t graphicsQueueFamilyIndex,
                uint32_t presentQueueFamilyIndex,
                std::string *errorMessage);
    void destroy(VkDevice device);

    VkSwapchainKHR handle() const;
    VkFormat format() const;
    VkExtent2D extent() const;
    const std::vector<VkImageView> &imageViews() const;

private:
    VkSwapchainKHR swapchain_ = VK_NULL_HANDLE;
    VkFormat format_ = VK_FORMAT_UNDEFINED;
    VkExtent2D extent_{};
    std::vector<VkImage> images_;
    std::vector<VkImageView> imageViews_;
};

} // namespace nativelib::render
