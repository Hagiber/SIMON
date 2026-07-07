#include "native_window_handle.h"

#include <algorithm>
#include <cstdint>

#include <android/native_window_jni.h>
#include <vulkan/vulkan_android.h>

namespace nativelib::platform::android {

NativeWindowHandle::~NativeWindowHandle() {
    reset();
}

std::array<const char *, 2> NativeWindowHandle::requiredInstanceExtensions() {
    return {
            VK_KHR_SURFACE_EXTENSION_NAME,
            VK_KHR_ANDROID_SURFACE_EXTENSION_NAME,
    };
}

bool NativeWindowHandle::create(JNIEnv *env,
                                jobject surfaceObj,
                                VkInstance instance,
                                std::string *errorMessage) {
    reset();

    if (env == nullptr || surfaceObj == nullptr) {
        if (errorMessage != nullptr) {
            *errorMessage = "Android surface is null";
        }
        return false;
    }
    if (instance == VK_NULL_HANDLE) {
        if (errorMessage != nullptr) {
            *errorMessage = "Vulkan instance is not initialized";
        }
        return false;
    }

    window_ = ANativeWindow_fromSurface(env, surfaceObj);
    if (window_ == nullptr) {
        if (errorMessage != nullptr) {
            *errorMessage = "ANativeWindow_fromSurface failed";
        }
        return false;
    }

    instance_ = instance;

    VkAndroidSurfaceCreateInfoKHR createInfo{VK_STRUCTURE_TYPE_ANDROID_SURFACE_CREATE_INFO_KHR};
    createInfo.window = window_;

    VkResult result = vkCreateAndroidSurfaceKHR(instance_, &createInfo, nullptr, &surface_);
    if (result != VK_SUCCESS) {
        reset();
        if (errorMessage != nullptr) {
            *errorMessage = "vkCreateAndroidSurfaceKHR failed";
        }
        return false;
    }

    return true;
}

void NativeWindowHandle::reset() {
    if (surface_ != VK_NULL_HANDLE && instance_ != VK_NULL_HANDLE) {
        vkDestroySurfaceKHR(instance_, surface_, nullptr);
    }
    surface_ = VK_NULL_HANDLE;
    instance_ = VK_NULL_HANDLE;

    if (window_ != nullptr) {
        ANativeWindow_release(window_);
        window_ = nullptr;
    }
}

ANativeWindow *NativeWindowHandle::window() const {
    return window_;
}

VkSurfaceKHR NativeWindowHandle::surface() const {
    return surface_;
}

VkExtent2D NativeWindowHandle::extent() const {
    if (window_ == nullptr) {
        return {};
    }

    const int32_t width = ANativeWindow_getWidth(window_);
    const int32_t height = ANativeWindow_getHeight(window_);
    return {
            static_cast<uint32_t>(std::max(width, int32_t{0})),
            static_cast<uint32_t>(std::max(height, int32_t{0})),
    };
}

} // namespace nativelib::platform::android
