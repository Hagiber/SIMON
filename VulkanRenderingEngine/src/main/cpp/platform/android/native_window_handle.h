#pragma once

#include <array>
#include <string>

#include <jni.h>
#include <vulkan/vulkan.h>

struct ANativeWindow;

namespace nativelib::platform::android {

class NativeWindowHandle {
public:
    NativeWindowHandle() = default;
    ~NativeWindowHandle();

    NativeWindowHandle(const NativeWindowHandle &) = delete;
    NativeWindowHandle &operator=(const NativeWindowHandle &) = delete;
    NativeWindowHandle(NativeWindowHandle &&) = delete;
    NativeWindowHandle &operator=(NativeWindowHandle &&) = delete;

    static std::array<const char *, 2> requiredInstanceExtensions();

    bool create(JNIEnv *env, jobject surfaceObj, VkInstance instance, std::string *errorMessage);
    void reset();

    ANativeWindow *window() const;
    VkSurfaceKHR surface() const;
    VkExtent2D extent() const;

private:
    ANativeWindow *window_ = nullptr;
    VkInstance instance_ = VK_NULL_HANDLE;
    VkSurfaceKHR surface_ = VK_NULL_HANDLE;
};

} // namespace nativelib::platform::android
