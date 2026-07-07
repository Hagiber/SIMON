#include "vulkan_device.h"

#include <algorithm>
#include <array>
#include <cstring>
#include <string>
#include <vector>

#include <swappy/swappyVk.h>

#include "../platform/android/native_window_handle.h"

namespace nativelib::render {
namespace {

bool fail(std::string *errorMessage, const char *message) {
    if (errorMessage != nullptr) {
        *errorMessage = message;
    }
    return false;
}

const char *vkResultName(VkResult result) {
    switch (result) {
        case VK_SUCCESS:
            return "VK_SUCCESS";
        case VK_ERROR_SURFACE_LOST_KHR:
            return "VK_ERROR_SURFACE_LOST_KHR";
        case VK_ERROR_DEVICE_LOST:
            return "VK_ERROR_DEVICE_LOST";
        case VK_ERROR_OUT_OF_HOST_MEMORY:
            return "VK_ERROR_OUT_OF_HOST_MEMORY";
        case VK_ERROR_OUT_OF_DEVICE_MEMORY:
            return "VK_ERROR_OUT_OF_DEVICE_MEMORY";
        case VK_ERROR_EXTENSION_NOT_PRESENT:
            return "VK_ERROR_EXTENSION_NOT_PRESENT";
        default:
            return "VK_ERROR_UNKNOWN";
    }
}

bool failVk(std::string *errorMessage, const char *operation, VkResult result) {
    if (errorMessage != nullptr) {
        *errorMessage = std::string(operation) + " failed: " + vkResultName(result);
    }
    return false;
}

bool supportsSwapchainExtension(VkPhysicalDevice device) {
    uint32_t extensionCount = 0;
    VkResult result = vkEnumerateDeviceExtensionProperties(device, nullptr, &extensionCount, nullptr);
    if (result != VK_SUCCESS) {
        return false;
    }
    if (extensionCount == 0) {
        return false;
    }

    std::vector<VkExtensionProperties> extensions(extensionCount);
    result = vkEnumerateDeviceExtensionProperties(device, nullptr, &extensionCount, extensions.data());
    if (result != VK_SUCCESS) {
        return false;
    }

    for (const VkExtensionProperties &extension : extensions) {
        if (std::strcmp(extension.extensionName, VK_KHR_SWAPCHAIN_EXTENSION_NAME) == 0) {
            return true;
        }
    }
    return false;
}

bool hasUsableSwapchainSurface(VkPhysicalDevice device, VkSurfaceKHR surface) {
    uint32_t formatCount = 0;
    VkResult result = vkGetPhysicalDeviceSurfaceFormatsKHR(device, surface, &formatCount, nullptr);
    if (result != VK_SUCCESS || formatCount == 0) {
        return false;
    }

    uint32_t presentModeCount = 0;
    result = vkGetPhysicalDeviceSurfacePresentModesKHR(device, surface, &presentModeCount, nullptr);
    return result == VK_SUCCESS && presentModeCount > 0;
}

} // namespace

bool VulkanDevice::createInstance(std::string *errorMessage) {
    const auto extensions = nativelib::platform::android::NativeWindowHandle::requiredInstanceExtensions();

    VkApplicationInfo appInfo{VK_STRUCTURE_TYPE_APPLICATION_INFO};
    appInfo.pApplicationName = "SIMON Vulkan Screen";
    appInfo.applicationVersion = VK_MAKE_VERSION(1, 0, 0);
    appInfo.pEngineName = "SIMON2D";
    appInfo.engineVersion = VK_MAKE_VERSION(1, 0, 0);
    appInfo.apiVersion = VK_API_VERSION_1_0;

    VkInstanceCreateInfo createInfo{VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO};
    createInfo.pApplicationInfo = &appInfo;
    createInfo.enabledExtensionCount = static_cast<uint32_t>(extensions.size());
    createInfo.ppEnabledExtensionNames = extensions.data();

    VkResult result = vkCreateInstance(&createInfo, nullptr, &instance_);
    if (result != VK_SUCCESS) {
        return failVk(errorMessage, "vkCreateInstance", result);
    }

    return true;
}

bool VulkanDevice::pickPhysicalDevice(VkSurfaceKHR surface, std::string *errorMessage) {
    if (surface == VK_NULL_HANDLE) {
        return fail(errorMessage, "No Vulkan surface available");
    }

    uint32_t deviceCount = 0;
    VkResult result = vkEnumeratePhysicalDevices(instance_, &deviceCount, nullptr);
    if (result != VK_SUCCESS) {
        return failVk(errorMessage, "vkEnumeratePhysicalDevices", result);
    }
    if (deviceCount == 0) {
        return fail(errorMessage, "No Vulkan physical device found");
    }

    std::vector<VkPhysicalDevice> devices(deviceCount);
    result = vkEnumeratePhysicalDevices(instance_, &deviceCount, devices.data());
    if (result != VK_SUCCESS) {
        return failVk(errorMessage, "vkEnumeratePhysicalDevices", result);
    }

    for (auto device : devices) {
        if (!supportsSwapchainExtension(device) || !hasUsableSwapchainSurface(device, surface)) {
            continue;
        }

        uint32_t queueFamilyCount = 0;
        vkGetPhysicalDeviceQueueFamilyProperties(device, &queueFamilyCount, nullptr);

        std::vector<VkQueueFamilyProperties> queueFamilies(queueFamilyCount);
        vkGetPhysicalDeviceQueueFamilyProperties(device, &queueFamilyCount, queueFamilies.data());

        uint32_t graphicsIndex = UINT32_MAX;
        uint32_t presentIndex = UINT32_MAX;

        for (uint32_t i = 0; i < queueFamilyCount; ++i) {
            if (queueFamilies[i].queueFlags & VK_QUEUE_GRAPHICS_BIT) {
                graphicsIndex = i;
            }

            VkBool32 presentSupport = VK_FALSE;
            result = vkGetPhysicalDeviceSurfaceSupportKHR(device, i, surface, &presentSupport);
            if (result != VK_SUCCESS) {
                presentSupport = VK_FALSE;
            }
            if (presentSupport) {
                presentIndex = i;
            }

            if (graphicsIndex != UINT32_MAX && presentIndex != UINT32_MAX) {
                break;
            }
        }

        if (graphicsIndex != UINT32_MAX && presentIndex != UINT32_MAX) {
            physicalDevice_ = device;
            graphicsQueueFamilyIndex_ = graphicsIndex;
            presentQueueFamilyIndex_ = presentIndex;
            return true;
        }
    }

    return fail(errorMessage, "No suitable Vulkan device with swapchain presentation support found");
}

bool VulkanDevice::createLogicalDevice(std::string *errorMessage) {
    float queuePriority = 1.0f;
    std::vector<VkDeviceQueueCreateInfo> queueCreateInfos;

    VkDeviceQueueCreateInfo graphicsQueueInfo{VK_STRUCTURE_TYPE_DEVICE_QUEUE_CREATE_INFO};
    graphicsQueueInfo.queueFamilyIndex = graphicsQueueFamilyIndex_;
    graphicsQueueInfo.queueCount = 1;
    graphicsQueueInfo.pQueuePriorities = &queuePriority;
    queueCreateInfos.push_back(graphicsQueueInfo);

    if (presentQueueFamilyIndex_ != graphicsQueueFamilyIndex_) {
        VkDeviceQueueCreateInfo presentQueueInfo{VK_STRUCTURE_TYPE_DEVICE_QUEUE_CREATE_INFO};
        presentQueueInfo.queueFamilyIndex = presentQueueFamilyIndex_;
        presentQueueInfo.queueCount = 1;
        presentQueueInfo.pQueuePriorities = &queuePriority;
        queueCreateInfos.push_back(presentQueueInfo);
    }

    uint32_t availableExtensionCount = 0;
    VkResult extensionResult = vkEnumerateDeviceExtensionProperties(
            physicalDevice_, nullptr, &availableExtensionCount, nullptr);
    if (extensionResult != VK_SUCCESS) {
        return failVk(errorMessage, "vkEnumerateDeviceExtensionProperties", extensionResult);
    }

    std::vector<VkExtensionProperties> availableExtensions(availableExtensionCount);
    extensionResult = vkEnumerateDeviceExtensionProperties(
            physicalDevice_, nullptr, &availableExtensionCount, availableExtensions.data());
    if (extensionResult != VK_SUCCESS) {
        return failVk(errorMessage, "vkEnumerateDeviceExtensionProperties", extensionResult);
    }

    uint32_t swappyExtensionCount = 0;
    SwappyVk_determineDeviceExtensions(physicalDevice_,
                                       availableExtensionCount,
                                       availableExtensions.data(),
                                       &swappyExtensionCount,
                                       nullptr);

    std::vector<std::array<char, VK_MAX_EXTENSION_NAME_SIZE>> swappyExtensionStorage(
            swappyExtensionCount);
    std::vector<char *> swappyExtensionPointers;
    swappyExtensionPointers.reserve(swappyExtensionCount);
    for (auto &extensionName : swappyExtensionStorage) {
        extensionName.fill('\0');
        swappyExtensionPointers.push_back(extensionName.data());
    }
    if (swappyExtensionCount > 0) {
        SwappyVk_determineDeviceExtensions(physicalDevice_,
                                           availableExtensionCount,
                                           availableExtensions.data(),
                                           &swappyExtensionCount,
                                           swappyExtensionPointers.data());
    }

    std::vector<std::string> deviceExtensionNames = {
            VK_KHR_SWAPCHAIN_EXTENSION_NAME,
    };
    for (uint32_t i = 0; i < swappyExtensionCount; ++i) {
        std::string extensionName(swappyExtensionPointers[i]);
        if (!extensionName.empty() &&
            std::find(deviceExtensionNames.begin(),
                      deviceExtensionNames.end(),
                      extensionName) == deviceExtensionNames.end()) {
            deviceExtensionNames.push_back(std::move(extensionName));
        }
    }

    std::vector<const char *> deviceExtensions;
    deviceExtensions.reserve(deviceExtensionNames.size());
    for (const std::string &extensionName : deviceExtensionNames) {
        deviceExtensions.push_back(extensionName.c_str());
    }

    VkDeviceCreateInfo createInfo{VK_STRUCTURE_TYPE_DEVICE_CREATE_INFO};
    createInfo.queueCreateInfoCount = static_cast<uint32_t>(queueCreateInfos.size());
    createInfo.pQueueCreateInfos = queueCreateInfos.data();
    createInfo.enabledExtensionCount = static_cast<uint32_t>(deviceExtensions.size());
    createInfo.ppEnabledExtensionNames = deviceExtensions.data();

    VkResult result = vkCreateDevice(physicalDevice_, &createInfo, nullptr, &device_);
    if (result != VK_SUCCESS) {
        return failVk(errorMessage, "vkCreateDevice", result);
    }

    vkGetDeviceQueue(device_, graphicsQueueFamilyIndex_, 0, &graphicsQueue_);
    vkGetDeviceQueue(device_, presentQueueFamilyIndex_, 0, &presentQueue_);
    SwappyVk_setQueueFamilyIndex(device_, presentQueue_, presentQueueFamilyIndex_);
    return true;
}

void VulkanDevice::destroyLogicalDevice() {
    if (device_ != VK_NULL_HANDLE) {
        SwappyVk_destroyDevice(device_);
        vkDestroyDevice(device_, nullptr);
        device_ = VK_NULL_HANDLE;
    }
    graphicsQueue_ = VK_NULL_HANDLE;
    presentQueue_ = VK_NULL_HANDLE;
}

void VulkanDevice::destroyInstance() {
    if (instance_ != VK_NULL_HANDLE) {
        vkDestroyInstance(instance_, nullptr);
        instance_ = VK_NULL_HANDLE;
    }
    physicalDevice_ = VK_NULL_HANDLE;
    graphicsQueueFamilyIndex_ = UINT32_MAX;
    presentQueueFamilyIndex_ = UINT32_MAX;
}

void VulkanDevice::destroy() {
    destroyLogicalDevice();
    destroyInstance();
}

uint32_t VulkanDevice::findMemoryType(uint32_t typeFilter, VkMemoryPropertyFlags properties) const {
    VkPhysicalDeviceMemoryProperties memProperties{};
    vkGetPhysicalDeviceMemoryProperties(physicalDevice_, &memProperties);

    for (uint32_t i = 0; i < memProperties.memoryTypeCount; i++) {
        if ((typeFilter & (1u << i)) &&
            (memProperties.memoryTypes[i].propertyFlags & properties) == properties) {
            return i;
        }
    }

    return UINT32_MAX;
}

bool VulkanDevice::createBuffer(VkDeviceSize size,
                                VkBufferUsageFlags usage,
                                VkMemoryPropertyFlags properties,
                                VkBuffer &buffer,
                                VkDeviceMemory &memory,
                                std::string *errorMessage) const {
    VkBufferCreateInfo bufferInfo{VK_STRUCTURE_TYPE_BUFFER_CREATE_INFO};
    bufferInfo.size = size;
    bufferInfo.usage = usage;
    bufferInfo.sharingMode = VK_SHARING_MODE_EXCLUSIVE;

    if (vkCreateBuffer(device_, &bufferInfo, nullptr, &buffer) != VK_SUCCESS) {
        return fail(errorMessage, "Failed to create buffer");
    }

    VkMemoryRequirements memRequirements{};
    vkGetBufferMemoryRequirements(device_, buffer, &memRequirements);

    VkMemoryAllocateInfo allocInfo{VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO};
    allocInfo.allocationSize = memRequirements.size;
    allocInfo.memoryTypeIndex = findMemoryType(memRequirements.memoryTypeBits, properties);
    if (allocInfo.memoryTypeIndex == UINT32_MAX) {
        vkDestroyBuffer(device_, buffer, nullptr);
        buffer = VK_NULL_HANDLE;
        return fail(errorMessage, "No suitable buffer memory type");
    }

    if (vkAllocateMemory(device_, &allocInfo, nullptr, &memory) != VK_SUCCESS) {
        vkDestroyBuffer(device_, buffer, nullptr);
        buffer = VK_NULL_HANDLE;
        return fail(errorMessage, "Failed to allocate buffer memory");
    }

    vkBindBufferMemory(device_, buffer, memory, 0);
    return true;
}

bool VulkanDevice::createImage(uint32_t width,
                               uint32_t height,
                               VkFormat format,
                               VkImageTiling tiling,
                               VkImageUsageFlags usage,
                               VkMemoryPropertyFlags properties,
                               VkImage &image,
                               VkDeviceMemory &memory,
                               std::string *errorMessage) const {
    VkImageCreateInfo imageInfo{VK_STRUCTURE_TYPE_IMAGE_CREATE_INFO};
    imageInfo.imageType = VK_IMAGE_TYPE_2D;
    imageInfo.extent.width = width;
    imageInfo.extent.height = height;
    imageInfo.extent.depth = 1;
    imageInfo.mipLevels = 1;
    imageInfo.arrayLayers = 1;
    imageInfo.format = format;
    imageInfo.tiling = tiling;
    imageInfo.initialLayout = VK_IMAGE_LAYOUT_UNDEFINED;
    imageInfo.usage = usage;
    imageInfo.samples = VK_SAMPLE_COUNT_1_BIT;
    imageInfo.sharingMode = VK_SHARING_MODE_EXCLUSIVE;

    if (vkCreateImage(device_, &imageInfo, nullptr, &image) != VK_SUCCESS) {
        return fail(errorMessage, "Failed to create image");
    }

    VkMemoryRequirements memRequirements{};
    vkGetImageMemoryRequirements(device_, image, &memRequirements);

    VkMemoryAllocateInfo allocInfo{VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO};
    allocInfo.allocationSize = memRequirements.size;
    allocInfo.memoryTypeIndex = findMemoryType(memRequirements.memoryTypeBits, properties);
    if (allocInfo.memoryTypeIndex == UINT32_MAX) {
        vkDestroyImage(device_, image, nullptr);
        image = VK_NULL_HANDLE;
        return fail(errorMessage, "No suitable image memory type");
    }

    if (vkAllocateMemory(device_, &allocInfo, nullptr, &memory) != VK_SUCCESS) {
        vkDestroyImage(device_, image, nullptr);
        image = VK_NULL_HANDLE;
        return fail(errorMessage, "Failed to allocate image memory");
    }

    vkBindImageMemory(device_, image, memory, 0);
    return true;
}

VkInstance VulkanDevice::instance() const {
    return instance_;
}

VkPhysicalDevice VulkanDevice::physicalDevice() const {
    return physicalDevice_;
}

VkDevice VulkanDevice::device() const {
    return device_;
}

VkQueue VulkanDevice::graphicsQueue() const {
    return graphicsQueue_;
}

VkQueue VulkanDevice::presentQueue() const {
    return presentQueue_;
}

uint32_t VulkanDevice::graphicsQueueFamilyIndex() const {
    return graphicsQueueFamilyIndex_;
}

uint32_t VulkanDevice::presentQueueFamilyIndex() const {
    return presentQueueFamilyIndex_;
}

} // namespace nativelib::render
