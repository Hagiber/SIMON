#include "vulkan_swapchain.h"

#include <algorithm>

#include "vulkan_types.h"

namespace nativelib::render {
namespace {

const char *vkResultName(VkResult result) {
    switch (result) {
        case VK_SUCCESS:
            return "VK_SUCCESS";
        case VK_SUBOPTIMAL_KHR:
            return "VK_SUBOPTIMAL_KHR";
        case VK_ERROR_OUT_OF_DATE_KHR:
            return "VK_ERROR_OUT_OF_DATE_KHR";
        case VK_ERROR_SURFACE_LOST_KHR:
            return "VK_ERROR_SURFACE_LOST_KHR";
        case VK_ERROR_DEVICE_LOST:
            return "VK_ERROR_DEVICE_LOST";
        case VK_ERROR_OUT_OF_HOST_MEMORY:
            return "VK_ERROR_OUT_OF_HOST_MEMORY";
        case VK_ERROR_OUT_OF_DEVICE_MEMORY:
            return "VK_ERROR_OUT_OF_DEVICE_MEMORY";
        case VK_ERROR_NATIVE_WINDOW_IN_USE_KHR:
            return "VK_ERROR_NATIVE_WINDOW_IN_USE_KHR";
        default:
            return "VK_ERROR_UNKNOWN";
    }
}

bool fail(std::string *errorMessage, const std::string &message) {
    if (errorMessage != nullptr) {
        *errorMessage = message;
    }
    return false;
}

bool failVk(std::string *errorMessage, const char *operation, VkResult result) {
    return fail(errorMessage, std::string(operation) + " failed: " + vkResultName(result));
}

} // namespace

bool VulkanSwapchain::create(VkPhysicalDevice physicalDevice,
                             VkDevice device,
                             VkSurfaceKHR surface,
                             VkExtent2D fallbackExtent,
                             uint32_t graphicsQueueFamilyIndex,
                             uint32_t presentQueueFamilyIndex,
                             std::string *errorMessage) {
    destroy(device);

    if (surface == VK_NULL_HANDLE) {
        return fail(errorMessage, "No Vulkan surface available");
    }

    VkSurfaceCapabilitiesKHR capabilities{};
    VkResult result = vkGetPhysicalDeviceSurfaceCapabilitiesKHR(physicalDevice, surface, &capabilities);
    if (result != VK_SUCCESS) {
        return failVk(errorMessage, "vkGetPhysicalDeviceSurfaceCapabilitiesKHR", result);
    }

    uint32_t formatCount = 0;
    result = vkGetPhysicalDeviceSurfaceFormatsKHR(physicalDevice, surface, &formatCount, nullptr);
    if (result != VK_SUCCESS) {
        return failVk(errorMessage, "vkGetPhysicalDeviceSurfaceFormatsKHR", result);
    }
    if (formatCount == 0) {
        return fail(errorMessage, "No surface formats available");
    }
    std::vector<VkSurfaceFormatKHR> formats(formatCount);
    result = vkGetPhysicalDeviceSurfaceFormatsKHR(physicalDevice, surface, &formatCount, formats.data());
    if (result != VK_SUCCESS) {
        return failVk(errorMessage, "vkGetPhysicalDeviceSurfaceFormatsKHR", result);
    }

    VkSurfaceFormatKHR chosenFormat = formats[0];
    for (const auto &format : formats) {
        if (format.format == VK_FORMAT_R8G8B8A8_UNORM || format.format == VK_FORMAT_B8G8R8A8_UNORM) {
            chosenFormat = format;
            break;
        }
    }

    const VkPresentModeKHR presentMode = VK_PRESENT_MODE_FIFO_KHR;

    VkExtent2D chosenExtent{};
    if (capabilities.currentExtent.width != UINT32_MAX) {
        chosenExtent = capabilities.currentExtent;
    } else {
        chosenExtent = fallbackExtent;
        chosenExtent.width = std::max(capabilities.minImageExtent.width,
                                      std::min(capabilities.maxImageExtent.width, chosenExtent.width));
        chosenExtent.height = std::max(capabilities.minImageExtent.height,
                                       std::min(capabilities.maxImageExtent.height, chosenExtent.height));
    }
    if (chosenExtent.width == 0 || chosenExtent.height == 0) {
        return fail(errorMessage, "Surface extent is zero; swapchain creation deferred");
    }

    uint32_t imageCount = std::max(capabilities.minImageCount + 1,
                                   kPreferredSwapchainImageCount);
    if (capabilities.maxImageCount > 0 && imageCount > capabilities.maxImageCount) {
        imageCount = capabilities.maxImageCount;
    }

    VkSurfaceTransformFlagBitsKHR preTransform = capabilities.currentTransform;
    if ((capabilities.supportedTransforms & VK_SURFACE_TRANSFORM_IDENTITY_BIT_KHR) != 0) {
        preTransform = VK_SURFACE_TRANSFORM_IDENTITY_BIT_KHR;
    }

    VkSwapchainCreateInfoKHR createInfo{VK_STRUCTURE_TYPE_SWAPCHAIN_CREATE_INFO_KHR};
    createInfo.surface = surface;
    createInfo.minImageCount = imageCount;
    createInfo.imageFormat = chosenFormat.format;
    createInfo.imageColorSpace = chosenFormat.colorSpace;
    createInfo.imageExtent = chosenExtent;
    createInfo.imageArrayLayers = 1;
    createInfo.imageUsage = VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT;

    uint32_t queueFamilyIndices[] = {graphicsQueueFamilyIndex, presentQueueFamilyIndex};
    if (graphicsQueueFamilyIndex != presentQueueFamilyIndex) {
        createInfo.imageSharingMode = VK_SHARING_MODE_CONCURRENT;
        createInfo.queueFamilyIndexCount = 2;
        createInfo.pQueueFamilyIndices = queueFamilyIndices;
    } else {
        createInfo.imageSharingMode = VK_SHARING_MODE_EXCLUSIVE;
    }

    createInfo.preTransform = preTransform;
    createInfo.compositeAlpha = VK_COMPOSITE_ALPHA_OPAQUE_BIT_KHR;
    createInfo.presentMode = presentMode;
    createInfo.clipped = VK_TRUE;
    createInfo.oldSwapchain = VK_NULL_HANDLE;

    result = vkCreateSwapchainKHR(device, &createInfo, nullptr, &swapchain_);
    if (result != VK_SUCCESS) {
        return failVk(errorMessage, "vkCreateSwapchainKHR", result);
    }

    format_ = chosenFormat.format;
    extent_ = chosenExtent;

    uint32_t swapchainImageCount = 0;
    result = vkGetSwapchainImagesKHR(device, swapchain_, &swapchainImageCount, nullptr);
    if (result != VK_SUCCESS || swapchainImageCount == 0) {
        destroy(device);
        if (result != VK_SUCCESS) {
            return failVk(errorMessage, "vkGetSwapchainImagesKHR", result);
        }
        return fail(errorMessage, "Swapchain created without images");
    }
    images_.resize(swapchainImageCount);
    result = vkGetSwapchainImagesKHR(device, swapchain_, &swapchainImageCount, images_.data());
    if (result != VK_SUCCESS) {
        destroy(device);
        return failVk(errorMessage, "vkGetSwapchainImagesKHR", result);
    }

    imageViews_.resize(images_.size());
    for (size_t i = 0; i < images_.size(); ++i) {
        VkImageViewCreateInfo viewInfo{VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO};
        viewInfo.image = images_[i];
        viewInfo.viewType = VK_IMAGE_VIEW_TYPE_2D;
        viewInfo.format = format_;
        viewInfo.subresourceRange.aspectMask = VK_IMAGE_ASPECT_COLOR_BIT;
        viewInfo.subresourceRange.baseMipLevel = 0;
        viewInfo.subresourceRange.levelCount = 1;
        viewInfo.subresourceRange.baseArrayLayer = 0;
        viewInfo.subresourceRange.layerCount = 1;

        result = vkCreateImageView(device, &viewInfo, nullptr, &imageViews_[i]);
        if (result != VK_SUCCESS) {
            destroy(device);
            return failVk(errorMessage, "vkCreateImageView", result);
        }
    }

    return true;
}

void VulkanSwapchain::destroy(VkDevice device) {
    if (device == VK_NULL_HANDLE) {
        return;
    }

    for (auto imageView : imageViews_) {
        if (imageView != VK_NULL_HANDLE) {
            vkDestroyImageView(device, imageView, nullptr);
        }
    }
    imageViews_.clear();
    images_.clear();

    if (swapchain_ != VK_NULL_HANDLE) {
        vkDestroySwapchainKHR(device, swapchain_, nullptr);
        swapchain_ = VK_NULL_HANDLE;
    }
    format_ = VK_FORMAT_UNDEFINED;
    extent_ = {};
}

VkSwapchainKHR VulkanSwapchain::handle() const {
    return swapchain_;
}

VkFormat VulkanSwapchain::format() const {
    return format_;
}

VkExtent2D VulkanSwapchain::extent() const {
    return extent_;
}

const std::vector<VkImageView> &VulkanSwapchain::imageViews() const {
    return imageViews_;
}

} // namespace nativelib::render
