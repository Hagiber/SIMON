#pragma once

#include <cstdint>
#include <string>

#include <vulkan/vulkan.h>

namespace nativelib::render {

class VulkanDevice {
public:
    bool createInstance(std::string *errorMessage);
    bool pickPhysicalDevice(VkSurfaceKHR surface, std::string *errorMessage);
    bool createLogicalDevice(std::string *errorMessage);

    void destroyLogicalDevice();
    void destroyInstance();
    void destroy();

    uint32_t findMemoryType(uint32_t typeFilter, VkMemoryPropertyFlags properties) const;
    bool createBuffer(VkDeviceSize size,
                      VkBufferUsageFlags usage,
                      VkMemoryPropertyFlags properties,
                      VkBuffer &buffer,
                      VkDeviceMemory &memory,
                      std::string *errorMessage) const;
    bool createImage(uint32_t width,
                     uint32_t height,
                     VkFormat format,
                     VkImageTiling tiling,
                     VkImageUsageFlags usage,
                     VkMemoryPropertyFlags properties,
                     VkImage &image,
                     VkDeviceMemory &memory,
                     std::string *errorMessage) const;

    VkInstance instance() const;
    VkPhysicalDevice physicalDevice() const;
    VkDevice device() const;
    VkQueue graphicsQueue() const;
    VkQueue presentQueue() const;
    uint32_t graphicsQueueFamilyIndex() const;
    uint32_t presentQueueFamilyIndex() const;

private:
    VkInstance instance_ = VK_NULL_HANDLE;
    VkPhysicalDevice physicalDevice_ = VK_NULL_HANDLE;
    VkDevice device_ = VK_NULL_HANDLE;
    VkQueue graphicsQueue_ = VK_NULL_HANDLE;
    VkQueue presentQueue_ = VK_NULL_HANDLE;
    uint32_t graphicsQueueFamilyIndex_ = UINT32_MAX;
    uint32_t presentQueueFamilyIndex_ = UINT32_MAX;
};

} // namespace nativelib::render
