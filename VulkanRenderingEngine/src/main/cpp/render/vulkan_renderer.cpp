#include "vulkan_renderer.h"

#include <algorithm>
#include <array>
#include <cmath>
#include <cstdint>
#include <cstring>
#include <initializer_list>
#include <limits>
#include <utility>

#include <android/asset_manager.h>
#include <android/asset_manager_jni.h>
#include <android/log.h>
#include <swappy/swappyVk.h>

#include "vulkan_render_engine.h"

namespace {

constexpr const char *kTag = "NativeLibVulkan";
constexpr uint32_t kTextureUploadsPerFrame = 2;
constexpr uint64_t kFenceWaitTimeoutNanos = 100'000'000ULL;
constexpr bool kUseSwappyFramePacing = false;

#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, kTag, __VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, kTag, __VA_ARGS__)

float lerp(float previous, float current, float alpha) {
    return previous + (current - previous) * alpha;
}

float lerpAngleDeg(float previous, float current, float alpha) {
    float delta = std::fmod(current - previous, 360.0f);
    if (delta > 180.0f) {
        delta -= 360.0f;
    } else if (delta < -180.0f) {
        delta += 360.0f;
    }
    return previous + delta * alpha;
}

bool allFinite(std::initializer_list<float> values) {
    for (float value : values) {
        if (!std::isfinite(value)) {
            return false;
        }
    }
    return true;
}

bool isValidBlendMode(jint value) {
    return value >= 0 &&
           static_cast<std::size_t>(value) < nativelib::kBlendModeCount;
}

bool isValidTextureSlot(jint value) {
    return value >= 0 &&
           static_cast<uint32_t>(value) <= nativelib::kMaxSceneTextureSlot;
}

bool validateSceneSpriteCount(jsize spriteCount, std::string *errorMessage) {
    if (spriteCount > static_cast<jsize>(nativelib::kMaxSceneSprites)) {
        if (errorMessage != nullptr) {
            *errorMessage = "Scene frame sprite count " + std::to_string(spriteCount) +
                            " exceeds max " + std::to_string(nativelib::kMaxSceneSprites);
        }
        return false;
    }
    return true;
}

bool validateSceneArray(JNIEnv *env,
                        jarray array,
                        const char *fieldName,
                        jsize expectedLength,
                        const char *expectedFieldName,
                        std::string *errorMessage) {
    if (array == nullptr) {
        if (errorMessage != nullptr) {
            *errorMessage = std::string("Scene frame array ") + fieldName + " must not be null";
        }
        return false;
    }

    jsize actualLength = env->GetArrayLength(array);
    if (actualLength != expectedLength) {
        if (errorMessage != nullptr) {
            *errorMessage = std::string("Scene frame array ") + fieldName +
                            " length " + std::to_string(actualLength) +
                            " does not match " + expectedFieldName +
                            " length " + std::to_string(expectedLength);
        }
        return false;
    }
    return true;
}

std::string textureSlotMessage(const char *fieldName, jsize index, jint value) {
    return std::string("Scene frame ") + fieldName + "[" + std::to_string(index) +
           "] must be between 0 and " + std::to_string(nativelib::kMaxSceneTextureSlot) +
           " but was " + std::to_string(value);
}

std::string blendModeMessage(const char *fieldName, jsize index, jint value) {
    return std::string("Scene frame ") + fieldName + "[" + std::to_string(index) +
           "] has unsupported value " + std::to_string(value) +
           " (expected 0.." + std::to_string(nativelib::kBlendModeCount - 1u) + ")";
}

nativelib::BlendMode toBlendMode(jint value) {
    return static_cast<nativelib::BlendMode>(static_cast<uint32_t>(value));
}

std::uint64_t packExtent(uint32_t width, uint32_t height) {
    return (static_cast<std::uint64_t>(width) << 32u) | static_cast<std::uint64_t>(height);
}

VkExtent2D unpackExtent(std::uint64_t packedExtent) {
    return {
            static_cast<uint32_t>(packedExtent >> 32u),
            static_cast<uint32_t>(packedExtent & 0xffffffffu)
    };
}

bool hasNonZeroExtent(VkExtent2D extent) {
    return extent.width > 0 && extent.height > 0;
}

std::string extentString(VkExtent2D extent) {
    return std::to_string(extent.width) + "x" + std::to_string(extent.height);
}

const char *vkResultName(VkResult result) {
    switch (result) {
        case VK_SUCCESS:
            return "VK_SUCCESS";
        case VK_TIMEOUT:
            return "VK_TIMEOUT";
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
        case VK_ERROR_MEMORY_MAP_FAILED:
            return "VK_ERROR_MEMORY_MAP_FAILED";
        default:
            return "VK_ERROR_UNKNOWN";
    }
}

std::string vkFailureMessage(const char *operation, VkResult result) {
    return std::string(operation) + " failed: " + vkResultName(result);
}

const char *presentOperationName() {
    return kUseSwappyFramePacing ? "SwappyVk_queuePresent" : "vkQueuePresentKHR";
}

bool isDeviceLost(VkResult result) {
    return result == VK_ERROR_DEVICE_LOST;
}

bool isSurfaceLost(VkResult result) {
    return result == VK_ERROR_SURFACE_LOST_KHR;
}

bool isValidScissor(jint x, jint y, jint width, jint height) {
    if (width == 0 && height == 0) {
        return true;
    }
    return x >= 0 && y >= 0 && width > 0 && height > 0;
}

nativelib::RenderScissor toRenderScissor(jint x, jint y, jint width, jint height) {
    nativelib::RenderScissor scissor{};
    if (width == 0 && height == 0) {
        return scissor;
    }

    scissor.enabled = true;
    scissor.x = x;
    scissor.y = y;
    scissor.width = static_cast<uint32_t>(width);
    scissor.height = static_cast<uint32_t>(height);
    return scissor;
}

bool isValidTextureRegion(jfloat u, jfloat v, jfloat widthUv, jfloat heightUv) {
    constexpr float kTextureRegionEpsilon = 0.0001f;
    return allFinite({u, v, widthUv, heightUv}) &&
           u >= 0.0f &&
           v >= 0.0f &&
           widthUv > 0.0f &&
           heightUv > 0.0f &&
           u + widthUv <= 1.0f + kTextureRegionEpsilon &&
           v + heightUv <= 1.0f + kTextureRegionEpsilon;
}

nativelib::render::VulkanRenderer gRenderer;

} // namespace

namespace nativelib::render {

bool VulkanRenderer::init(JNIEnv *env,
                          jobject activityObj,
                          jobject surfaceObj,
                          jobject assetManagerObj) {
    std::lock_guard<std::mutex> lock(resourceMutex_);

    if (initialized_.load(std::memory_order_acquire)) {
        destroyLocked();
    }

    if (!initLocked(env, activityObj, surfaceObj, assetManagerObj)) {
        destroyLocked();
        return false;
    }

    return true;
}

bool VulkanRenderer::initLocked(JNIEnv *env,
                                jobject activityObj,
                                jobject surfaceObj,
                                jobject assetManagerObj) {
    clearError();
    if (env == nullptr || activityObj == nullptr || assetManagerObj == nullptr) {
        return fail("Android activity or asset manager is null");
    }
    if (env->GetJavaVM(&javaVm_) != JNI_OK || javaVm_ == nullptr) {
        return fail("Failed to obtain Java VM for Swappy");
    }
    activity_ = env->NewGlobalRef(activityObj);
    if (activity_ == nullptr) {
        return fail("Failed to retain Android activity for Swappy");
    }
    assetManager_ = AAssetManager_fromJava(env, assetManagerObj);
    if (assetManager_ == nullptr) {
        return fail("Android asset manager is null");
    }

    std::string errorMessage;
    if (!device_.createInstance(&errorMessage)) {
        return fail(errorMessage);
    }

    if (!createSurface(env, surfaceObj)) {
        return false;
    }

    if (!device_.pickPhysicalDevice(windowHandle_.surface(), &errorMessage)) {
        return fail(errorMessage);
    }

    if (!device_.createLogicalDevice(&errorMessage)) {
        return fail(errorMessage);
    }

    if (!frameResources_.createCommandPool(device_.device(),
                                           device_.graphicsQueueFamilyIndex(),
                                           &errorMessage)) {
        return fail(errorMessage);
    }

    if (!frameResources_.createSyncObjects(device_.device(), &errorMessage)) {
        return fail(errorMessage);
    }

    textureStreamCache_.reset();
    spriteBatcher_.reset();
    if (!spriteBatcher_.create(device_, &errorMessage)) {
        return fail(errorMessage);
    }
    textureUploadRequests_ = 0;
    textureUploadsProcessed_ = 0;
    atlasUploads_ = 0;
    swapchainRecreates_ = 0;
    lastTextureUploadStatus_ = "idle";
    lastSwapchainStatus_ = "creating";
    currentFrameIndex_ = 0;

    if (!descriptorResources_.create(device_, textures_, &errorMessage)) {
        return fail(errorMessage);
    }

    if (!createVertexBuffer()) {
        return false;
    }

    if (!createSwapchainObjects()) {
        return false;
    }

    clearPendingSceneSnapshot();
    sceneGeneration_.fetch_add(1, std::memory_order_acq_rel);
    initialized_.store(true, std::memory_order_release);
    LOGI("Vulkan initialized framesInFlight=%u swapchainImages=%zu swappyRefresh=%lluns",
         kMaxFramesInFlight,
         swapchain_.imageViews().size(),
         static_cast<unsigned long long>(swappyRefreshDurationNanos_));
    return true;
}

bool VulkanRenderer::renderFrame() {
    std::lock_guard<std::mutex> lock(resourceMutex_);
    if (!initialized_.load(std::memory_order_acquire)) {
        setError("Engine is not initialized");
        return false;
    }
    clearError();

    VkDevice device = device_.device();
    if (device == VK_NULL_HANDLE) {
        return failAndResetDevice("Vulkan device is not initialized");
    }

    const uint32_t frameIndex = currentFrameIndex_;
    VkFence inFlightFence = frameResources_.inFlightFence(frameIndex);
    if (inFlightFence == VK_NULL_HANDLE) {
        return failAndResetDevice("Vulkan frame fence is not initialized");
    }

    VkResult fenceResult = vkWaitForFences(device, 1, &inFlightFence, VK_TRUE, kFenceWaitTimeoutNanos);
    if (fenceResult == VK_TIMEOUT) {
        setError("Timed out waiting for in-flight frame");
        return false;
    }
    if (fenceResult != VK_SUCCESS) {
        if (isDeviceLost(fenceResult)) {
            return failAndResetDevice(vkFailureMessage("vkWaitForFences", fenceResult));
        }
        setError(vkFailureMessage("vkWaitForFences", fenceResult));
        return false;
    }

    SwapchainStatus swapchainStatus = ensureSwapchainReady();
    if (swapchainStatus == SwapchainStatus::Deferred) {
        return true;
    }
    if (swapchainStatus == SwapchainStatus::Failed) {
        return false;
    }

    if (!processPendingTextureUploads()) {
        return false;
    }
    if (!consumePendingSceneSnapshotLocked()) {
        return false;
    }

    std::string frameUpdateError;
    if (!descriptorResources_.updateCamera(device,
                                           frameIndex,
                                           swapchain_.extent(),
                                           sceneCamera_,
                                           &frameUpdateError)) {
        return fail(frameUpdateError.empty() ? "Failed to update frame camera" : frameUpdateError);
    }
    if (!spriteBatcher_.uploadInstances(device, frameIndex, &frameUpdateError)) {
        return fail(frameUpdateError);
    }

    uint32_t imageIndex = 0;
    VkResult acquireResult = vkAcquireNextImageKHR(
            device,
            swapchain_.handle(),
            UINT64_MAX,
            frameResources_.imageAvailableSemaphore(frameIndex),
            VK_NULL_HANDLE,
            &imageIndex);

    if (acquireResult == VK_ERROR_OUT_OF_DATE_KHR) {
        framebufferResized_.store(true, std::memory_order_release);
        lastSwapchainStatus_ = "acquire out-of-date, resize pending";
        return true;
    }
    if (isSurfaceLost(acquireResult) || isDeviceLost(acquireResult)) {
        return failAndResetDevice(vkFailureMessage("vkAcquireNextImageKHR", acquireResult));
    }

    if (acquireResult != VK_SUCCESS && acquireResult != VK_SUBOPTIMAL_KHR) {
        setError(vkFailureMessage("vkAcquireNextImageKHR", acquireResult));
        return false;
    }
    if (acquireResult == VK_SUBOPTIMAL_KHR) {
        lastSwapchainStatus_ = "acquire suboptimal";
    }
    if (imageIndex >= frameResources_.swapchainImageCount()) {
        setError("Swapchain image index is outside command buffers");
        framebufferResized_.store(true, std::memory_order_release);
        return false;
    }

    VkFence imageInFlightFence = frameResources_.imageInFlightFence(imageIndex);
    if (imageInFlightFence != VK_NULL_HANDLE && imageInFlightFence != inFlightFence) {
        VkResult imageFenceResult =
                vkWaitForFences(device, 1, &imageInFlightFence, VK_TRUE, kFenceWaitTimeoutNanos);
        if (imageFenceResult == VK_TIMEOUT) {
            setError("Timed out waiting for swapchain image");
            return false;
        }
        if (imageFenceResult != VK_SUCCESS) {
            if (isDeviceLost(imageFenceResult)) {
                return failAndResetDevice(vkFailureMessage("vkWaitForFences(image)", imageFenceResult));
            }
            setError(vkFailureMessage("vkWaitForFences(image)", imageFenceResult));
            return false;
        }
    }

    VkResult resetResult = vkResetFences(device, 1, &inFlightFence);
    if (resetResult != VK_SUCCESS) {
        if (isDeviceLost(resetResult)) {
            return failAndResetDevice(vkFailureMessage("vkResetFences", resetResult));
        }
        setError(vkFailureMessage("vkResetFences", resetResult));
        return false;
    }
    frameResources_.setImageInFlightFence(imageIndex, inFlightFence);

    VkPipelineStageFlags waitStages[] = {VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT};
    VkSemaphore imageAvailableSemaphore = frameResources_.imageAvailableSemaphore(frameIndex);
    VkSemaphore renderFinishedSemaphore = frameResources_.renderFinishedSemaphore(frameIndex);
    VkCommandBuffer commandBuffer = frameResources_.commandBuffer(frameIndex, imageIndex);

    VkSubmitInfo submitInfo{VK_STRUCTURE_TYPE_SUBMIT_INFO};
    submitInfo.waitSemaphoreCount = 1;
    submitInfo.pWaitSemaphores = &imageAvailableSemaphore;
    submitInfo.pWaitDstStageMask = waitStages;
    submitInfo.commandBufferCount = 1;
    submitInfo.pCommandBuffers = &commandBuffer;
    submitInfo.signalSemaphoreCount = 1;
    submitInfo.pSignalSemaphores = &renderFinishedSemaphore;

    VkResult submitResult = vkQueueSubmit(device_.graphicsQueue(), 1, &submitInfo, inFlightFence);
    if (submitResult != VK_SUCCESS) {
        return failAndResetDevice(vkFailureMessage("vkQueueSubmit", submitResult));
    }
    currentFrameIndex_ = (currentFrameIndex_ + 1u) % kMaxFramesInFlight;

    VkPresentInfoKHR presentInfo{VK_STRUCTURE_TYPE_PRESENT_INFO_KHR};
    presentInfo.waitSemaphoreCount = 1;
    presentInfo.pWaitSemaphores = &renderFinishedSemaphore;

    VkSwapchainKHR swapchains[] = {swapchain_.handle()};
    presentInfo.swapchainCount = 1;
    presentInfo.pSwapchains = swapchains;
    presentInfo.pImageIndices = &imageIndex;

    VkResult presentResult = kUseSwappyFramePacing
                             ? SwappyVk_queuePresent(device_.presentQueue(), &presentInfo)
                             : vkQueuePresentKHR(device_.presentQueue(), &presentInfo);
    if (presentResult == VK_ERROR_OUT_OF_DATE_KHR) {
        framebufferResized_.store(true, std::memory_order_release);
        lastSwapchainStatus_ = "present out-of-date, resize pending";
        return true;
    } else if (isSurfaceLost(presentResult) || isDeviceLost(presentResult)) {
        return failAndResetDevice(vkFailureMessage(presentOperationName(), presentResult));
    } else if (presentResult != VK_SUCCESS && presentResult != VK_SUBOPTIMAL_KHR) {
        setError(vkFailureMessage(presentOperationName(), presentResult));
        return false;
    } else if (presentResult == VK_SUBOPTIMAL_KHR) {
        lastSwapchainStatus_ = "present suboptimal";
    }
    return true;
}

void VulkanRenderer::resize(jint width, jint height) {
    std::lock_guard<std::mutex> lock(resourceMutex_);

    jint clampedWidth = std::max(width, 0);
    jint clampedHeight = std::max(height, 0);
    bool extentChanged = rememberSurfaceExtent(clampedWidth, clampedHeight);
    VkExtent2D requestedExtent{
            static_cast<uint32_t>(clampedWidth),
            static_cast<uint32_t>(clampedHeight)
    };
    lastSwapchainStatus_ = "resize requested " + extentString(requestedExtent);
    if (!initialized_.load(std::memory_order_acquire)) {
        return;
    }

    if (clampedWidth == 0 || clampedHeight == 0) {
        framebufferResized_.store(true, std::memory_order_release);
        lastSwapchainStatus_ = "resize deferred " + extentString(requestedExtent);
        return;
    }

    if (extentChanged) {
        framebufferResized_.store(true, std::memory_order_release);
    }
}

void VulkanRenderer::cleanup() {
    std::lock_guard<std::mutex> lock(resourceMutex_);
    destroyLocked();
}

std::string VulkanRenderer::lastError() const {
    std::lock_guard<std::mutex> lock(errorMutex_);
    return lastError_;
}

std::string VulkanRenderer::textureUploadStatus() {
    std::lock_guard<std::mutex> lock(resourceMutex_);

    std::string atlasStatus = "atlas=none";
    if (spriteBatcher_.hasAtlasTexture()) {
        atlasStatus = "atlas=" +
                      std::to_string(spriteBatcher_.atlasWidth()) +
                      "x" +
                      std::to_string(spriteBatcher_.atlasHeight());
    }

    bool gpuTextureReady = kSpriteAtlasTextureSlot < textures_.size() &&
                           textures_[kSpriteAtlasTextureSlot].imageView != VK_NULL_HANDLE &&
                           textures_[kSpriteAtlasTextureSlot].sampler != VK_NULL_HANDLE;
    return lastTextureUploadStatus_ +
           ", requests=" +
           std::to_string(textureUploadRequests_) +
           ", processed=" +
           std::to_string(textureUploadsProcessed_) +
           ", pending=" +
           std::to_string(textureStreamCache_.pendingUploadCount()) +
           ", slots=" +
           std::to_string(textureStreamCache_.reservedSlotCount()) +
           ", " +
           atlasStatus +
           ", gpu=" +
           (gpuTextureReady ? "ready" : "not-ready");
}

std::string VulkanRenderer::swapchainStatus() {
    std::lock_guard<std::mutex> lock(resourceMutex_);

    VkExtent2D requestedExtent = unpackExtent(surfaceExtent_.load(std::memory_order_acquire));
    VkExtent2D swapchainExtent = swapchain_.extent();
    bool initialized = initialized_.load(std::memory_order_acquire);
    bool resizePending = framebufferResized_.load(std::memory_order_acquire);
    bool ready = initialized &&
                 swapchain_.handle() != VK_NULL_HANDLE &&
                 hasNonZeroExtent(swapchainExtent);

    return lastSwapchainStatus_ +
           ", ready=" +
           (ready ? "yes" : "no") +
           ", requested=" +
           extentString(requestedExtent) +
           ", swapchain=" +
           extentString(swapchainExtent) +
           ", images=" +
           std::to_string(swapchain_.imageViews().size()) +
           ", resizePending=" +
           (resizePending ? "yes" : "no") +
           ", recreates=" +
           std::to_string(swapchainRecreates_);
}

jint VulkanRenderer::uploadTextureRgba(JNIEnv *env,
                                       jstring debugName,
                                       jint width,
                                       jint height,
                                       jobject rgbaBuffer) {
    std::lock_guard<std::mutex> lock(resourceMutex_);

    if (!initialized_.load(std::memory_order_acquire)) {
        setError("Engine is not initialized");
        return -1;
    }

    uint8_t *pixels = nullptr;
    size_t byteCount = 0;
    if (!validateTextureUpload(env, width, height, rgbaBuffer, &pixels, &byteCount)) {
        return -1;
    }

    std::string name = readDebugName(env, debugName);
    std::string errorMessage;
    TextureStreamCache::EnqueueResult enqueueResult{};
    if (!textureStreamCache_.enqueueAuto(name,
                                         static_cast<uint32_t>(width),
                                         static_cast<uint32_t>(height),
                                         pixels,
                                         byteCount,
                                         &enqueueResult,
                                         &errorMessage)) {
        setError(errorMessage);
        return -1;
    }

    LOGI("uploadTextureRgba(auto): %s (%dx%d) slot=%u %s",
         name.empty() ? "unnamed" : name.c_str(),
         width,
         height,
         enqueueResult.slot,
         enqueueResult.cacheHit ? "cache-hit" : "queued");

    if (enqueueResult.queued) {
        ++textureUploadRequests_;
    }
    lastTextureUploadStatus_ = std::string(enqueueResult.cacheHit ? "cache hit slot " : "queued slot ") +
                               std::to_string(enqueueResult.slot) +
                               " (" +
                               std::to_string(width) +
                               "x" +
                               std::to_string(height) +
                               ")";

    return static_cast<jint>(enqueueResult.slot + 1);
}

jint VulkanRenderer::uploadTextureRgbaToSlot(JNIEnv *env,
                                             jstring debugName,
                                             jint width,
                                             jint height,
                                             jobject rgbaBuffer,
                                             jint slot) {
    std::lock_guard<std::mutex> lock(resourceMutex_);

    if (!initialized_.load(std::memory_order_acquire)) {
        setError("Engine is not initialized");
        return -1;
    }
    if (!isValidTextureSlot(slot)) {
        setError("Texture slot must be between 0 and " +
                 std::to_string(nativelib::kMaxSceneTextureSlot) +
                 " but was " +
                 std::to_string(slot));
        return -1;
    }

    uint8_t *pixels = nullptr;
    size_t byteCount = 0;
    if (!validateTextureUpload(env, width, height, rgbaBuffer, &pixels, &byteCount)) {
        return -1;
    }

    std::string name = readDebugName(env, debugName);
    std::string errorMessage;
    TextureStreamCache::EnqueueResult enqueueResult{};
    if (!textureStreamCache_.enqueueToSlot(name,
                                           static_cast<uint32_t>(width),
                                           static_cast<uint32_t>(height),
                                           pixels,
                                           byteCount,
                                           static_cast<uint32_t>(slot),
                                           &enqueueResult,
                                           &errorMessage)) {
        setError(errorMessage);
        return -1;
    }

    LOGI("uploadTextureRgba: %s (%dx%d) slot=%u %s",
         name.empty() ? "unnamed" : name.c_str(),
         width,
         height,
         enqueueResult.slot,
         enqueueResult.cacheHit ? "cache-hit" : "queued");

    if (enqueueResult.queued) {
        ++textureUploadRequests_;
    }
    lastTextureUploadStatus_ = std::string(enqueueResult.cacheHit ? "cache hit slot " : "queued slot ") +
                               std::to_string(enqueueResult.slot) +
                               " (" +
                               std::to_string(width) +
                               "x" +
                               std::to_string(height) +
                               ")";

    return static_cast<jint>(enqueueResult.slot + 1);
}

jboolean VulkanRenderer::releaseTexture(jint slot) {
    std::lock_guard<std::mutex> lock(resourceMutex_);

    if (!initialized_.load(std::memory_order_acquire)) {
        setError("Engine is not initialized");
        return JNI_FALSE;
    }
    if (!isValidTextureSlot(slot)) {
        setError("Texture slot must be between 0 and " +
                 std::to_string(nativelib::kMaxSceneTextureSlot) +
                 " but was " +
                 std::to_string(slot));
        return JNI_FALSE;
    }

    VkDevice device = device_.device();
    if (device != VK_NULL_HANDLE) {
        vkDeviceWaitIdle(device);
    }

    std::string errorMessage;
    if (!textureStreamCache_.releaseSlot(static_cast<uint32_t>(slot), spriteBatcher_, &errorMessage)) {
        fail(errorMessage);
        return JNI_FALSE;
    }
    return uploadAtlasTexture() ? JNI_TRUE : JNI_FALSE;
}

void VulkanRenderer::clearTextureCache() {
    std::lock_guard<std::mutex> lock(resourceMutex_);

    if (!initialized_.load(std::memory_order_acquire)) {
        textureStreamCache_.clear(spriteBatcher_);
        return;
    }

    VkDevice device = device_.device();
    if (device != VK_NULL_HANDLE) {
        vkDeviceWaitIdle(device);
    }

    textureStreamCache_.clear(spriteBatcher_);
    uploadAtlasTexture();
}

jboolean VulkanRenderer::submitReadOnlySceneFrame(JNIEnv *env,
                                                  jint payloadVersion,
                                                  jintArray previousTextureSlots,
                                                  jintArray previousBlendModes,
                                                  jintArray previousLayers,
                                                  jintArray previousRenderOrders,
                                                  jintArray previousScissorXs,
                                                  jintArray previousScissorYs,
                                                  jintArray previousScissorWidths,
                                                  jintArray previousScissorHeights,
                                                  jfloatArray previousTextureUs,
                                                  jfloatArray previousTextureVs,
                                                  jfloatArray previousTextureWidthUvs,
                                                  jfloatArray previousTextureHeightUvs,
                                                  jfloatArray previousXs,
                                                  jfloatArray previousYs,
                                                  jfloatArray previousZs,
                                                  jfloatArray previousScaleXs,
                                                  jfloatArray previousScaleYs,
                                                  jfloatArray previousRotationDegs,
                                                  jfloatArray previousAnimationStates,
                                                  jintArray currentTextureSlots,
                                                  jintArray currentBlendModes,
                                                  jintArray currentLayers,
                                                  jintArray currentRenderOrders,
                                                  jintArray currentScissorXs,
                                                  jintArray currentScissorYs,
                                                  jintArray currentScissorWidths,
                                                  jintArray currentScissorHeights,
                                                  jfloatArray currentTextureUs,
                                                  jfloatArray currentTextureVs,
                                                  jfloatArray currentTextureWidthUvs,
                                                  jfloatArray currentTextureHeightUvs,
                                                  jfloatArray currentXs,
                                                  jfloatArray currentYs,
                                                  jfloatArray currentZs,
                                                  jfloatArray currentScaleXs,
                                                  jfloatArray currentScaleYs,
                                                  jfloatArray currentRotationDegs,
                                                  jfloatArray currentAnimationStates,
                                                  jfloat previousCameraX,
                                                  jfloat previousCameraY,
                                                  jfloat previousCameraZoom,
                                                  jfloat previousCameraRotationDeg,
                                                  jfloat currentCameraX,
                                                  jfloat currentCameraY,
                                                  jfloat currentCameraZoom,
                                                  jfloat currentCameraRotationDeg,
                                                  jfloat interpolationAlpha) {
    if (!initialized_.load(std::memory_order_acquire)) {
        setError("Engine is not initialized");
        return JNI_FALSE;
    }
    if (payloadVersion != nativelib::kSceneFramePayloadVersion) {
        setError("Scene frame payload version mismatch: got " +
                 std::to_string(payloadVersion) +
                 ", expected " +
                 std::to_string(nativelib::kSceneFramePayloadVersion));
        return JNI_FALSE;
    }
    std::uint64_t snapshotGeneration = sceneGeneration_.load(std::memory_order_acquire);
    if (previousTextureSlots == nullptr) {
        setError("Scene frame array previousTextureSlots must not be null");
        return JNI_FALSE;
    }
    if (!std::isfinite(interpolationAlpha) || interpolationAlpha < 0.0f || interpolationAlpha > 1.0f) {
        setError("Scene frame interpolation alpha must be in [0, 1]");
        return JNI_FALSE;
    }
    if (!allFinite({previousCameraX,
                    previousCameraY,
                    previousCameraZoom,
                    previousCameraRotationDeg,
                    currentCameraX,
                    currentCameraY,
                    currentCameraZoom,
                    currentCameraRotationDeg}) ||
        previousCameraZoom <= 0.0f ||
        currentCameraZoom <= 0.0f) {
        setError("Scene frame camera values are invalid");
        return JNI_FALSE;
    }

    jsize spriteCount = env->GetArrayLength(previousTextureSlots);
    std::string validationError;
    if (!validateSceneSpriteCount(spriteCount, &validationError) ||
        !validateSceneArray(env, previousBlendModes, "previousBlendModes", spriteCount, "previousTextureSlots", &validationError) ||
        !validateSceneArray(env, previousLayers, "previousLayers", spriteCount, "previousTextureSlots", &validationError) ||
        !validateSceneArray(env, previousRenderOrders, "previousRenderOrders", spriteCount, "previousTextureSlots", &validationError) ||
        !validateSceneArray(env, previousScissorXs, "previousScissorXs", spriteCount, "previousTextureSlots", &validationError) ||
        !validateSceneArray(env, previousScissorYs, "previousScissorYs", spriteCount, "previousTextureSlots", &validationError) ||
        !validateSceneArray(env, previousScissorWidths, "previousScissorWidths", spriteCount, "previousTextureSlots", &validationError) ||
        !validateSceneArray(env, previousScissorHeights, "previousScissorHeights", spriteCount, "previousTextureSlots", &validationError) ||
        !validateSceneArray(env, previousTextureUs, "previousTextureUs", spriteCount, "previousTextureSlots", &validationError) ||
        !validateSceneArray(env, previousTextureVs, "previousTextureVs", spriteCount, "previousTextureSlots", &validationError) ||
        !validateSceneArray(env, previousTextureWidthUvs, "previousTextureWidthUvs", spriteCount, "previousTextureSlots", &validationError) ||
        !validateSceneArray(env, previousTextureHeightUvs, "previousTextureHeightUvs", spriteCount, "previousTextureSlots", &validationError) ||
        !validateSceneArray(env, previousXs, "previousXs", spriteCount, "previousTextureSlots", &validationError) ||
        !validateSceneArray(env, previousYs, "previousYs", spriteCount, "previousTextureSlots", &validationError) ||
        !validateSceneArray(env, previousZs, "previousZs", spriteCount, "previousTextureSlots", &validationError) ||
        !validateSceneArray(env, previousScaleXs, "previousScaleXs", spriteCount, "previousTextureSlots", &validationError) ||
        !validateSceneArray(env, previousScaleYs, "previousScaleYs", spriteCount, "previousTextureSlots", &validationError) ||
        !validateSceneArray(env, previousRotationDegs, "previousRotationDegs", spriteCount, "previousTextureSlots", &validationError) ||
        !validateSceneArray(env, previousAnimationStates, "previousAnimationStates", spriteCount, "previousTextureSlots", &validationError) ||
        !validateSceneArray(env, currentTextureSlots, "currentTextureSlots", spriteCount, "previousTextureSlots", &validationError) ||
        !validateSceneArray(env, currentBlendModes, "currentBlendModes", spriteCount, "previousTextureSlots", &validationError) ||
        !validateSceneArray(env, currentLayers, "currentLayers", spriteCount, "previousTextureSlots", &validationError) ||
        !validateSceneArray(env, currentRenderOrders, "currentRenderOrders", spriteCount, "previousTextureSlots", &validationError) ||
        !validateSceneArray(env, currentScissorXs, "currentScissorXs", spriteCount, "previousTextureSlots", &validationError) ||
        !validateSceneArray(env, currentScissorYs, "currentScissorYs", spriteCount, "previousTextureSlots", &validationError) ||
        !validateSceneArray(env, currentScissorWidths, "currentScissorWidths", spriteCount, "previousTextureSlots", &validationError) ||
        !validateSceneArray(env, currentScissorHeights, "currentScissorHeights", spriteCount, "previousTextureSlots", &validationError) ||
        !validateSceneArray(env, currentTextureUs, "currentTextureUs", spriteCount, "previousTextureSlots", &validationError) ||
        !validateSceneArray(env, currentTextureVs, "currentTextureVs", spriteCount, "previousTextureSlots", &validationError) ||
        !validateSceneArray(env, currentTextureWidthUvs, "currentTextureWidthUvs", spriteCount, "previousTextureSlots", &validationError) ||
        !validateSceneArray(env, currentTextureHeightUvs, "currentTextureHeightUvs", spriteCount, "previousTextureSlots", &validationError) ||
        !validateSceneArray(env, currentXs, "currentXs", spriteCount, "previousTextureSlots", &validationError) ||
        !validateSceneArray(env, currentYs, "currentYs", spriteCount, "previousTextureSlots", &validationError) ||
        !validateSceneArray(env, currentZs, "currentZs", spriteCount, "previousTextureSlots", &validationError) ||
        !validateSceneArray(env, currentScaleXs, "currentScaleXs", spriteCount, "previousTextureSlots", &validationError) ||
        !validateSceneArray(env, currentScaleYs, "currentScaleYs", spriteCount, "previousTextureSlots", &validationError) ||
        !validateSceneArray(env, currentRotationDegs, "currentRotationDegs", spriteCount, "previousTextureSlots", &validationError) ||
        !validateSceneArray(env, currentAnimationStates, "currentAnimationStates", spriteCount, "previousTextureSlots", &validationError)) {
        setError(validationError);
        return JNI_FALSE;
    }

    std::vector<jint> previousSlotValues(static_cast<size_t>(spriteCount));
    std::vector<jint> previousBlendModeValues(static_cast<size_t>(spriteCount));
    std::vector<jint> previousLayerValues(static_cast<size_t>(spriteCount));
    std::vector<jint> previousRenderOrderValues(static_cast<size_t>(spriteCount));
    std::vector<jint> previousScissorXValues(static_cast<size_t>(spriteCount));
    std::vector<jint> previousScissorYValues(static_cast<size_t>(spriteCount));
    std::vector<jint> previousScissorWidthValues(static_cast<size_t>(spriteCount));
    std::vector<jint> previousScissorHeightValues(static_cast<size_t>(spriteCount));
    std::vector<jfloat> previousTextureUValues(static_cast<size_t>(spriteCount));
    std::vector<jfloat> previousTextureVValues(static_cast<size_t>(spriteCount));
    std::vector<jfloat> previousTextureWidthUvValues(static_cast<size_t>(spriteCount));
    std::vector<jfloat> previousTextureHeightUvValues(static_cast<size_t>(spriteCount));
    std::vector<jfloat> previousXValues(static_cast<size_t>(spriteCount));
    std::vector<jfloat> previousYValues(static_cast<size_t>(spriteCount));
    std::vector<jfloat> previousZValues(static_cast<size_t>(spriteCount));
    std::vector<jfloat> previousScaleXValues(static_cast<size_t>(spriteCount));
    std::vector<jfloat> previousScaleYValues(static_cast<size_t>(spriteCount));
    std::vector<jfloat> previousRotationValues(static_cast<size_t>(spriteCount));
    std::vector<jfloat> previousAnimationValues(static_cast<size_t>(spriteCount));
    std::vector<jint> currentSlotValues(static_cast<size_t>(spriteCount));
    std::vector<jint> currentBlendModeValues(static_cast<size_t>(spriteCount));
    std::vector<jint> currentLayerValues(static_cast<size_t>(spriteCount));
    std::vector<jint> currentRenderOrderValues(static_cast<size_t>(spriteCount));
    std::vector<jint> currentScissorXValues(static_cast<size_t>(spriteCount));
    std::vector<jint> currentScissorYValues(static_cast<size_t>(spriteCount));
    std::vector<jint> currentScissorWidthValues(static_cast<size_t>(spriteCount));
    std::vector<jint> currentScissorHeightValues(static_cast<size_t>(spriteCount));
    std::vector<jfloat> currentTextureUValues(static_cast<size_t>(spriteCount));
    std::vector<jfloat> currentTextureVValues(static_cast<size_t>(spriteCount));
    std::vector<jfloat> currentTextureWidthUvValues(static_cast<size_t>(spriteCount));
    std::vector<jfloat> currentTextureHeightUvValues(static_cast<size_t>(spriteCount));
    std::vector<jfloat> currentXValues(static_cast<size_t>(spriteCount));
    std::vector<jfloat> currentYValues(static_cast<size_t>(spriteCount));
    std::vector<jfloat> currentZValues(static_cast<size_t>(spriteCount));
    std::vector<jfloat> currentScaleXValues(static_cast<size_t>(spriteCount));
    std::vector<jfloat> currentScaleYValues(static_cast<size_t>(spriteCount));
    std::vector<jfloat> currentRotationValues(static_cast<size_t>(spriteCount));
    std::vector<jfloat> currentAnimationValues(static_cast<size_t>(spriteCount));

    // Read the host-owned immutable frame once; native never writes back into Java snapshot arrays.
    if (spriteCount > 0) {
        env->GetIntArrayRegion(previousTextureSlots, 0, spriteCount, previousSlotValues.data());
        env->GetIntArrayRegion(previousBlendModes, 0, spriteCount, previousBlendModeValues.data());
        env->GetIntArrayRegion(previousLayers, 0, spriteCount, previousLayerValues.data());
        env->GetIntArrayRegion(previousRenderOrders, 0, spriteCount, previousRenderOrderValues.data());
        env->GetIntArrayRegion(previousScissorXs, 0, spriteCount, previousScissorXValues.data());
        env->GetIntArrayRegion(previousScissorYs, 0, spriteCount, previousScissorYValues.data());
        env->GetIntArrayRegion(previousScissorWidths, 0, spriteCount, previousScissorWidthValues.data());
        env->GetIntArrayRegion(previousScissorHeights, 0, spriteCount, previousScissorHeightValues.data());
        env->GetFloatArrayRegion(previousTextureUs, 0, spriteCount, previousTextureUValues.data());
        env->GetFloatArrayRegion(previousTextureVs, 0, spriteCount, previousTextureVValues.data());
        env->GetFloatArrayRegion(previousTextureWidthUvs, 0, spriteCount, previousTextureWidthUvValues.data());
        env->GetFloatArrayRegion(previousTextureHeightUvs, 0, spriteCount, previousTextureHeightUvValues.data());
        env->GetFloatArrayRegion(previousXs, 0, spriteCount, previousXValues.data());
        env->GetFloatArrayRegion(previousYs, 0, spriteCount, previousYValues.data());
        env->GetFloatArrayRegion(previousZs, 0, spriteCount, previousZValues.data());
        env->GetFloatArrayRegion(previousScaleXs, 0, spriteCount, previousScaleXValues.data());
        env->GetFloatArrayRegion(previousScaleYs, 0, spriteCount, previousScaleYValues.data());
        env->GetFloatArrayRegion(previousRotationDegs, 0, spriteCount, previousRotationValues.data());
        env->GetFloatArrayRegion(previousAnimationStates, 0, spriteCount, previousAnimationValues.data());
        env->GetIntArrayRegion(currentTextureSlots, 0, spriteCount, currentSlotValues.data());
        env->GetIntArrayRegion(currentBlendModes, 0, spriteCount, currentBlendModeValues.data());
        env->GetIntArrayRegion(currentLayers, 0, spriteCount, currentLayerValues.data());
        env->GetIntArrayRegion(currentRenderOrders, 0, spriteCount, currentRenderOrderValues.data());
        env->GetIntArrayRegion(currentScissorXs, 0, spriteCount, currentScissorXValues.data());
        env->GetIntArrayRegion(currentScissorYs, 0, spriteCount, currentScissorYValues.data());
        env->GetIntArrayRegion(currentScissorWidths, 0, spriteCount, currentScissorWidthValues.data());
        env->GetIntArrayRegion(currentScissorHeights, 0, spriteCount, currentScissorHeightValues.data());
        env->GetFloatArrayRegion(currentTextureUs, 0, spriteCount, currentTextureUValues.data());
        env->GetFloatArrayRegion(currentTextureVs, 0, spriteCount, currentTextureVValues.data());
        env->GetFloatArrayRegion(currentTextureWidthUvs, 0, spriteCount, currentTextureWidthUvValues.data());
        env->GetFloatArrayRegion(currentTextureHeightUvs, 0, spriteCount, currentTextureHeightUvValues.data());
        env->GetFloatArrayRegion(currentXs, 0, spriteCount, currentXValues.data());
        env->GetFloatArrayRegion(currentYs, 0, spriteCount, currentYValues.data());
        env->GetFloatArrayRegion(currentZs, 0, spriteCount, currentZValues.data());
        env->GetFloatArrayRegion(currentScaleXs, 0, spriteCount, currentScaleXValues.data());
        env->GetFloatArrayRegion(currentScaleYs, 0, spriteCount, currentScaleYValues.data());
        env->GetFloatArrayRegion(currentRotationDegs, 0, spriteCount, currentRotationValues.data());
        env->GetFloatArrayRegion(currentAnimationStates, 0, spriteCount, currentAnimationValues.data());
    }

    std::vector<nativelib::RenderSprite> sceneSnapshot;
    sceneSnapshot.reserve(static_cast<size_t>(spriteCount));

    for (jsize i = 0; i < spriteCount; ++i) {
        size_t index = static_cast<size_t>(i);
        jint previousSlotValue = previousSlotValues[index];
        jint currentSlotValue = currentSlotValues[index];
        jint previousBlendModeValue = previousBlendModeValues[index];
        jint currentBlendModeValue = currentBlendModeValues[index];
        if (!isValidTextureSlot(previousSlotValue)) {
            setError(textureSlotMessage("previousTextureSlots", i, previousSlotValue));
            return JNI_FALSE;
        }
        if (!isValidTextureSlot(currentSlotValue)) {
            setError(textureSlotMessage("currentTextureSlots", i, currentSlotValue));
            return JNI_FALSE;
        }
        if (previousSlotValue != currentSlotValue) {
            setError("Scene frame textureSlots[" + std::to_string(i) +
                     "] changed between previous and current snapshots (previous=" +
                     std::to_string(previousSlotValue) +
                     ", current=" +
                     std::to_string(currentSlotValue) +
                     ")");
            return JNI_FALSE;
        }
        if (!isValidBlendMode(previousBlendModeValue)) {
            setError(blendModeMessage("previousBlendModes", i, previousBlendModeValue));
            return JNI_FALSE;
        }
        if (!isValidBlendMode(currentBlendModeValue)) {
            setError(blendModeMessage("currentBlendModes", i, currentBlendModeValue));
            return JNI_FALSE;
        }
        if (previousBlendModeValue != currentBlendModeValue) {
            setError("Scene frame blendModes[" + std::to_string(i) +
                     "] changed between previous and current snapshots (previous=" +
                     std::to_string(previousBlendModeValue) +
                     ", current=" +
                     std::to_string(currentBlendModeValue) +
                     ")");
            return JNI_FALSE;
        }
        if (!isValidScissor(previousScissorXValues[index],
                            previousScissorYValues[index],
                            previousScissorWidthValues[index],
                            previousScissorHeightValues[index]) ||
            !isValidScissor(currentScissorXValues[index],
                            currentScissorYValues[index],
                            currentScissorWidthValues[index],
                            currentScissorHeightValues[index])) {
            setError("Scene frame scissor rectangles are invalid");
            return JNI_FALSE;
        }
        if (!isValidTextureRegion(previousTextureUValues[index],
                                  previousTextureVValues[index],
                                  previousTextureWidthUvValues[index],
                                  previousTextureHeightUvValues[index]) ||
            !isValidTextureRegion(currentTextureUValues[index],
                                  currentTextureVValues[index],
                                  currentTextureWidthUvValues[index],
                                  currentTextureHeightUvValues[index])) {
            setError("Scene frame texture regions are invalid");
            return JNI_FALSE;
        }
        if (!allFinite({previousXValues[index],
                        previousYValues[index],
                        previousZValues[index],
                        previousScaleXValues[index],
                        previousScaleYValues[index],
                        previousRotationValues[index],
                        previousAnimationValues[index],
                        currentXValues[index],
                        currentYValues[index],
                        currentZValues[index],
                        currentScaleXValues[index],
                        currentScaleYValues[index],
                        currentRotationValues[index],
                        currentAnimationValues[index]})) {
            setError("Scene frame sprite values must be finite");
            return JNI_FALSE;
        }

        nativelib::RenderSprite sprite{};
        sprite.textureSlot = static_cast<uint32_t>(currentSlotValue);
        sprite.blendMode = toBlendMode(currentBlendModeValue);
        sprite.layer = currentLayerValues[index];
        sprite.renderOrder = currentRenderOrderValues[index];
        sprite.scissor = toRenderScissor(currentScissorXValues[index],
                                         currentScissorYValues[index],
                                         currentScissorWidthValues[index],
                                         currentScissorHeightValues[index]);
        sprite.textureRegion.u = currentTextureUValues[index];
        sprite.textureRegion.v = currentTextureVValues[index];
        sprite.textureRegion.widthUv = currentTextureWidthUvValues[index];
        sprite.textureRegion.heightUv = currentTextureHeightUvValues[index];
        sprite.transform.x = lerp(previousXValues[index], currentXValues[index], interpolationAlpha);
        sprite.transform.y = lerp(previousYValues[index], currentYValues[index], interpolationAlpha);
        sprite.transform.z = lerp(previousZValues[index], currentZValues[index], interpolationAlpha);
        sprite.transform.scaleX = lerp(previousScaleXValues[index], currentScaleXValues[index], interpolationAlpha);
        sprite.transform.scaleY = lerp(previousScaleYValues[index], currentScaleYValues[index], interpolationAlpha);
        sprite.transform.rotationDeg =
                lerpAngleDeg(previousRotationValues[index], currentRotationValues[index], interpolationAlpha);
        sprite.animationState = lerp(previousAnimationValues[index], currentAnimationValues[index], interpolationAlpha);
        sceneSnapshot.push_back(sprite);
    }

    nativelib::OrthoCamera camera{};
    camera.x = lerp(previousCameraX, currentCameraX, interpolationAlpha);
    camera.y = lerp(previousCameraY, currentCameraY, interpolationAlpha);
    camera.zoom = lerp(previousCameraZoom, currentCameraZoom, interpolationAlpha);
    camera.rotationDeg = lerpAngleDeg(previousCameraRotationDeg,
                                      currentCameraRotationDeg,
                                      interpolationAlpha);

    if (!initialized_.load(std::memory_order_acquire) ||
        snapshotGeneration != sceneGeneration_.load(std::memory_order_acquire)) {
        setError("Engine is not initialized");
        return JNI_FALSE;
    }

    std::shared_ptr<SubmittedSceneSnapshot> pendingSceneSnapshot =
            std::make_shared<SubmittedSceneSnapshot>();
    pendingSceneSnapshot->generation = snapshotGeneration;
    pendingSceneSnapshot->camera = camera;
    pendingSceneSnapshot->sprites = std::move(sceneSnapshot);
    publishPendingSceneSnapshot(pendingSceneSnapshot);
    return JNI_TRUE;
}

void VulkanRenderer::destroyLocked() {
    initialized_.store(false, std::memory_order_release);
    sceneGeneration_.fetch_add(1, std::memory_order_acq_rel);
    clearPendingSceneSnapshot();

    VkDevice device = device_.device();
    if (device != VK_NULL_HANDLE) {
        vkDeviceWaitIdle(device);
        clearSwapchainResources();
        spriteBatcher_.destroy(device);
        textureStreamCache_.reset();
        destroyAllTextures();
        textures_.clear();
        sceneSnapshot_.clear();
        sceneCamera_ = nativelib::OrthoCamera{};

        if (vertexBuffer_ != VK_NULL_HANDLE) {
            vkDestroyBuffer(device, vertexBuffer_, nullptr);
            vertexBuffer_ = VK_NULL_HANDLE;
        }
        if (vertexBufferMemory_ != VK_NULL_HANDLE) {
            vkFreeMemory(device, vertexBufferMemory_, nullptr);
            vertexBufferMemory_ = VK_NULL_HANDLE;
        }

        descriptorResources_.destroy(device);
        frameResources_.destroy(device);
        device_.destroyLogicalDevice();
    } else {
        textureStreamCache_.reset();
        spriteBatcher_.reset();
        textures_.clear();
        sceneSnapshot_.clear();
        sceneCamera_ = nativelib::OrthoCamera{};
    }

    windowHandle_.reset();
    device_.destroyInstance();
    releaseActivityReference();
    assetManager_ = nullptr;
    framebufferResized_.store(false, std::memory_order_release);
    surfaceExtent_.store(0, std::memory_order_release);
    lastTextureUploadStatus_ = "idle";
    lastSwapchainStatus_ = "uninitialized";
    currentFrameIndex_ = 0;
    swappyRefreshDurationNanos_ = 0;
}

bool VulkanRenderer::createSurface(JNIEnv *env, jobject surfaceObj) {
    std::string errorMessage;
    if (!windowHandle_.create(env, surfaceObj, device_.instance(), &errorMessage)) {
        return fail(errorMessage);
    }

    VkExtent2D initialExtent = windowHandle_.extent();
    if (initialExtent.width > 0 && initialExtent.height > 0) {
        surfaceExtent_.store(packExtent(initialExtent.width, initialExtent.height),
                             std::memory_order_release);
    }

    return true;
}

bool VulkanRenderer::createSwapchainObjects() {
    std::string errorMessage;
    VkExtent2D requestedExtent = surfaceExtentOr(windowHandle_.extent());
    if (!swapchain_.create(device_.physicalDevice(),
                           device_.device(),
                           windowHandle_.surface(),
                           requestedExtent,
                           device_.graphicsQueueFamilyIndex(),
                           device_.presentQueueFamilyIndex(),
                           &errorMessage)) {
        return fail(errorMessage);
    }

    if (!initializeSwappyForSwapchain()) {
        return false;
    }

    for (uint32_t frameIndex = 0; frameIndex < kMaxFramesInFlight; ++frameIndex) {
        if (!descriptorResources_.updateCamera(device_.device(),
                                               frameIndex,
                                               swapchain_.extent(),
                                               sceneCamera_,
                                               &errorMessage)) {
            return fail(errorMessage.empty() ? "Failed to update camera" : errorMessage);
        }
    }

    if (!renderPass_.create(device_.device(), swapchain_.format(), &errorMessage)) {
        return fail(errorMessage);
    }

    if (!frameResources_.createFramebuffers(device_.device(),
                                            renderPass_.handle(),
                                            swapchain_.imageViews(),
                                            swapchain_.extent(),
                                            &errorMessage)) {
        return fail(errorMessage);
    }

    if (!frameResources_.allocateCommandBuffers(device_.device(),
                                                swapchain_.imageViews().size(),
                                                &errorMessage)) {
        return fail(errorMessage);
    }

    auto vertCode = loadAssetBytes("textured_color.vert.spv");
    auto fragCode = loadAssetBytes("textured_color.frag.spv");
    if (vertCode.empty() || fragCode.empty()) {
        return fail("Shader assets missing. Ensure VulkanRenderingEngine/src/main/shaders is packaged as assets.");
    }

    if (!pipeline_.create(device_.device(),
                          swapchain_.extent(),
                          renderPass_.handle(),
                          descriptorResources_.cameraDescriptorSetLayout(),
                          descriptorResources_.textureDescriptorSetLayout(),
                          vertCode,
                          fragCode,
                          &errorMessage)) {
        return fail(errorMessage);
    }

    for (uint32_t slot = 0; slot < textures_.size(); ++slot) {
        descriptorResources_.updateTextureDescriptor(device_.device(), textures_, slot);
    }

    if (!recordCurrentCommandBuffers()) {
        return false;
    }

    lastSwapchainStatus_ = "ready " +
                           extentString(swapchain_.extent()) +
                           " images=" +
                           std::to_string(swapchain_.imageViews().size()) +
                           " framesInFlight=" +
                           std::to_string(kMaxFramesInFlight) +
                           " pacer=" +
                           (kUseSwappyFramePacing ? "swappy" : "java-choreographer") +
                           " swappyRefreshNs=" +
                           std::to_string(swappyRefreshDurationNanos_);
    return true;
}

bool VulkanRenderer::initializeSwappyForSwapchain() {
    if (!kUseSwappyFramePacing) {
        swappyRefreshDurationNanos_ = 0;
        swappySwapchainInitialized_ = false;
        return true;
    }

    JNIEnv *env = currentJniEnv();
    if (env == nullptr || activity_ == nullptr) {
        return fail("Swappy initialization requires an attached JNI thread and Activity");
    }

    uint64_t refreshDurationNanos = 0;
    if (!SwappyVk_initAndGetRefreshCycleDuration(env,
                                                 activity_,
                                                 device_.physicalDevice(),
                                                 device_.device(),
                                                 swapchain_.handle(),
                                                 &refreshDurationNanos)) {
        return fail("SwappyVk_initAndGetRefreshCycleDuration failed");
    }

    SwappyVk_setWindow(device_.device(), swapchain_.handle(), windowHandle_.window());
    SwappyVk_setAutoSwapInterval(true);
    SwappyVk_setAutoPipelineMode(true);
    SwappyVk_setSwapIntervalNS(device_.device(), swapchain_.handle(), refreshDurationNanos);
    SwappyVk_enableFramePacing(swapchain_.handle(), true);
    swappyRefreshDurationNanos_ = refreshDurationNanos;
    swappySwapchainInitialized_ = true;
    return true;
}

void VulkanRenderer::destroySwappySwapchain() {
    if (!swappySwapchainInitialized_) {
        return;
    }
    VkDevice device = device_.device();
    VkSwapchainKHR swapchain = swapchain_.handle();
    if (device != VK_NULL_HANDLE && swapchain != VK_NULL_HANDLE) {
        SwappyVk_destroySwapchain(device, swapchain);
    }
    swappySwapchainInitialized_ = false;
    swappyRefreshDurationNanos_ = 0;
}

JNIEnv *VulkanRenderer::currentJniEnv() const {
    if (javaVm_ == nullptr) {
        return nullptr;
    }
    JNIEnv *env = nullptr;
    if (javaVm_->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION_1_6) != JNI_OK) {
        return nullptr;
    }
    return env;
}

void VulkanRenderer::releaseActivityReference() {
    if (activity_ != nullptr) {
        JNIEnv *env = currentJniEnv();
        if (env != nullptr) {
            env->DeleteGlobalRef(activity_);
        }
        activity_ = nullptr;
    }
    javaVm_ = nullptr;
}

void VulkanRenderer::clearSwapchainResources() {
    VkDevice device = device_.device();
    if (device == VK_NULL_HANDLE) {
        return;
    }

    vkDeviceWaitIdle(device);
    frameResources_.clearSwapchainResources(device);
    pipeline_.destroy(device);
    renderPass_.destroy(device);
    destroySwappySwapchain();
    swapchain_.destroy(device);
}

VulkanRenderer::SwapchainStatus VulkanRenderer::ensureSwapchainReady() {
    bool swapchainMissing = swapchain_.handle() == VK_NULL_HANDLE ||
                            frameResources_.commandBufferCount() == 0;
    bool shouldRecreate = framebufferResized_.exchange(false, std::memory_order_acq_rel) ||
                          swapchainMissing;
    if (!shouldRecreate) {
        return SwapchainStatus::Ready;
    }

    return recreateSwapchain();
}

VulkanRenderer::SwapchainStatus VulkanRenderer::recreateSwapchain() {
    if (hasDeferredSurfaceExtent()) {
        clearSwapchainResources();
        framebufferResized_.store(true, std::memory_order_release);
        lastSwapchainStatus_ = "deferred zero extent";
        return SwapchainStatus::Deferred;
    }

    clearSwapchainResources();
    if (!createSwapchainObjects()) {
        std::string message = lastError();
        lastSwapchainStatus_ = "recreate failed";
        failAndResetDevice(message.empty() ? "Failed to recreate Vulkan swapchain" : message);
        return SwapchainStatus::Failed;
    }
    ++swapchainRecreates_;
    framebufferResized_.store(false, std::memory_order_release);
    return SwapchainStatus::Ready;
}

bool VulkanRenderer::rememberSurfaceExtent(jint width, jint height) {
    std::uint64_t packedExtent = packExtent(static_cast<uint32_t>(width), static_cast<uint32_t>(height));
    return surfaceExtent_.exchange(packedExtent, std::memory_order_acq_rel) != packedExtent;
}

VkExtent2D VulkanRenderer::surfaceExtentOr(VkExtent2D fallback) const {
    std::uint64_t packedExtent = surfaceExtent_.load(std::memory_order_acquire);
    if (packedExtent == 0) {
        return fallback;
    }

    VkExtent2D extent = unpackExtent(packedExtent);
    if (extent.width == 0 || extent.height == 0) {
        return fallback;
    }
    return extent;
}

bool VulkanRenderer::hasDeferredSurfaceExtent() const {
    return !hasNonZeroExtent(surfaceExtentOr(windowHandle_.extent()));
}

bool VulkanRenderer::failAndResetDevice(const std::string &message) {
    setError(message);
    destroyLocked();
    return false;
}

bool VulkanRenderer::waitForDeviceIdle(const char *operation) {
    VkDevice device = device_.device();
    if (device == VK_NULL_HANDLE) {
        return failAndResetDevice("Vulkan device is not initialized");
    }

    VkResult result = vkDeviceWaitIdle(device);
    if (result == VK_SUCCESS) {
        return true;
    }
    if (isDeviceLost(result)) {
        return failAndResetDevice(vkFailureMessage(operation, result));
    }
    return fail(vkFailureMessage(operation, result));
}

bool VulkanRenderer::createVertexBuffer() {
    constexpr std::array<Vertex, 6> vertices = {{
            {{-0.35f, -0.35f}, {0.0f, 0.0f}, {1, 1, 1, 1}},
            {{0.35f, -0.35f}, {1.0f, 0.0f}, {1, 1, 1, 1}},
            {{0.35f, 0.35f}, {1.0f, 1.0f}, {1, 1, 1, 1}},
            {{-0.35f, -0.35f}, {0.0f, 0.0f}, {1, 1, 1, 1}},
            {{0.35f, 0.35f}, {1.0f, 1.0f}, {1, 1, 1, 1}},
            {{-0.35f, 0.35f}, {0.0f, 1.0f}, {1, 1, 1, 1}},
    }};

    std::string errorMessage;
    VkDeviceSize size = sizeof(vertices);
    if (!device_.createBuffer(size,
                              VK_BUFFER_USAGE_VERTEX_BUFFER_BIT,
                              VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT,
                              vertexBuffer_,
                              vertexBufferMemory_,
                              &errorMessage)) {
        return fail(errorMessage);
    }

    void *mapped = nullptr;
    VkResult mapResult = vkMapMemory(device_.device(), vertexBufferMemory_, 0, size, 0, &mapped);
    if (mapResult != VK_SUCCESS || mapped == nullptr) {
        return fail(vkFailureMessage("vkMapMemory", mapResult));
    }
    std::memcpy(mapped, vertices.data(), static_cast<size_t>(size));
    vkUnmapMemory(device_.device(), vertexBufferMemory_);
    return true;
}

bool VulkanRenderer::recordCurrentCommandBuffers() {
    if (!waitForDeviceIdle("vkDeviceWaitIdle(record command buffers)")) {
        return false;
    }

    std::string errorMessage;
    if (!frameResources_.recordCommandBuffers(device_.device(),
                                              renderPass_.handle(),
                                              swapchain_.extent(),
                                              pipeline_,
                                              vertexBuffer_,
                                              spriteBatcher_.instanceBuffers(),
                                              descriptorResources_.cameraDescriptorSets(),
                                              descriptorResources_.textureDescriptorSets(),
                                              textures_,
                                              spriteBatcher_.batches(),
                                              &errorMessage)) {
        return fail(errorMessage);
    }

    spriteBatcher_.clearCommandLayoutDirty();
    return true;
}

bool VulkanRenderer::processPendingTextureUploads() {
    if (!textureStreamCache_.hasPendingUploads()) {
        return true;
    }

    std::string errorMessage;
    uint32_t processedCount = 0;
    if (!textureStreamCache_.processPendingUploads(spriteBatcher_,
                                                   kTextureUploadsPerFrame,
                                                   &processedCount,
                                                   &errorMessage)) {
        return fail(errorMessage);
    }

    if (processedCount == 0) {
        lastTextureUploadStatus_ = "pending " +
                                   std::to_string(textureStreamCache_.pendingUploadCount()) +
                                   " upload(s)";
        return true;
    }
    textureUploadsProcessed_ += processedCount;
    lastTextureUploadStatus_ = "streamed " +
                               std::to_string(processedCount) +
                               " upload(s), pending=" +
                               std::to_string(textureStreamCache_.pendingUploadCount());
    return uploadAtlasTexture();
}

bool VulkanRenderer::uploadAtlasTexture() {
    if (!waitForDeviceIdle("vkDeviceWaitIdle(upload atlas)")) {
        return false;
    }

    if (!spriteBatcher_.hasAtlasTexture()) {
        if (kSpriteAtlasTextureSlot < textures_.size()) {
            destroyTexture(kSpriteAtlasTextureSlot);
        }

        std::string errorMessage;
        if (!spriteBatcher_.build(sceneSnapshot_, &errorMessage)) {
            return fail(errorMessage);
        }
        lastTextureUploadStatus_ = "atlas empty";
        return recordCurrentCommandBuffers();
    }

    std::string errorMessage;
    if (!descriptorResources_.ensureTextureSlotCapacity(device_,
                                                        textures_,
                                                        kSpriteAtlasTextureSlot + 1,
                                                        &errorMessage)) {
        return fail(errorMessage);
    }

    if (!createTextureFromBytes(kSpriteAtlasTextureSlot,
                                spriteBatcher_.atlasPixels(),
                                spriteBatcher_.atlasWidth(),
                                spriteBatcher_.atlasHeight())) {
        return false;
    }

    descriptorResources_.updateTextureDescriptor(device_.device(), textures_, kSpriteAtlasTextureSlot);
    if (!spriteBatcher_.build(sceneSnapshot_, &errorMessage)) {
        return fail(errorMessage);
    }
    ++atlasUploads_;
    lastTextureUploadStatus_ = "atlas uploaded " +
                               std::to_string(spriteBatcher_.atlasWidth()) +
                               "x" +
                               std::to_string(spriteBatcher_.atlasHeight()) +
                               " (#" +
                               std::to_string(atlasUploads_) +
                               ")";
    return recordCurrentCommandBuffers();
}

bool VulkanRenderer::consumePendingSceneSnapshotLocked() {
    std::shared_ptr<const SubmittedSceneSnapshot> pendingSceneSnapshot = consumePendingSceneSnapshot();
    if (!pendingSceneSnapshot) {
        return true;
    }

    std::uint64_t currentGeneration = sceneGeneration_.load(std::memory_order_acquire);
    if (pendingSceneSnapshot->generation != currentGeneration) {
        return true;
    }

    std::string errorMessage;
    sceneCamera_ = pendingSceneSnapshot->camera;
    sceneSnapshot_ = pendingSceneSnapshot->sprites;
    if (!spriteBatcher_.build(sceneSnapshot_, &errorMessage)) {
        return fail(errorMessage);
    }

    if (spriteBatcher_.commandLayoutDirty()) {
        return recordCurrentCommandBuffers();
    }
    return true;
}

bool VulkanRenderer::validateTextureUpload(JNIEnv *env,
                                           jint width,
                                           jint height,
                                           jobject rgbaBuffer,
                                           uint8_t **pixels,
                                           size_t *byteCount) {
    if (pixels == nullptr || byteCount == nullptr) {
        setError("Texture upload output pointers are null");
        return false;
    }
    *pixels = nullptr;
    *byteCount = 0;

    if (env == nullptr) {
        setError("JNI environment is null");
        return false;
    }
    if (rgbaBuffer == nullptr) {
        setError("rgbaBuffer must be non-null");
        return false;
    }
    if (width <= 0 || height <= 0) {
        setError("Texture dimensions must be positive");
        return false;
    }

    uint64_t expectedByteCount = static_cast<uint64_t>(width) *
                                 static_cast<uint64_t>(height) *
                                 4u;
    if (expectedByteCount > static_cast<uint64_t>(std::numeric_limits<size_t>::max())) {
        setError("Texture input data is too large");
        return false;
    }

    jlong bufferCapacity = env->GetDirectBufferCapacity(rgbaBuffer);
    if (bufferCapacity < 0) {
        setError("rgbaBuffer must be a direct ByteBuffer");
        return false;
    }
    if (static_cast<uint64_t>(bufferCapacity) < expectedByteCount) {
        setError("rgbaBuffer capacity is smaller than texture dimensions");
        return false;
    }

    uint8_t *directPixels = static_cast<uint8_t *>(env->GetDirectBufferAddress(rgbaBuffer));
    if (directPixels == nullptr) {
        setError("rgbaBuffer must expose direct memory");
        return false;
    }

    *pixels = directPixels;
    *byteCount = static_cast<size_t>(expectedByteCount);
    return true;
}

std::string VulkanRenderer::readDebugName(JNIEnv *env, jstring debugName) const {
    if (env == nullptr || debugName == nullptr) {
        return {};
    }

    const char *chars = env->GetStringUTFChars(debugName, nullptr);
    if (chars == nullptr) {
        return {};
    }

    std::string value(chars);
    env->ReleaseStringUTFChars(debugName, chars);
    return value;
}

bool VulkanRenderer::createTextureFromBytes(uint32_t slot,
                                            const uint8_t *pixels,
                                            uint32_t width,
                                            uint32_t height) {
    if (pixels == nullptr || width == 0 || height == 0) {
        setError("Invalid texture input data");
        return false;
    }
    if (slot >= textures_.size()) {
        setError("Invalid texture slot");
        return false;
    }

    TextureResource &texture = textures_[slot];
    destroyTexture(slot);

    std::string errorMessage;
    VkDevice device = device_.device();
    VkDeviceSize imageSize = static_cast<VkDeviceSize>(width) * height * 4;

    VkBuffer stagingBuffer = VK_NULL_HANDLE;
    VkDeviceMemory stagingMemory = VK_NULL_HANDLE;
    if (!device_.createBuffer(imageSize,
                              VK_BUFFER_USAGE_TRANSFER_SRC_BIT,
                              VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT,
                              stagingBuffer,
                              stagingMemory,
                              &errorMessage)) {
        return fail(errorMessage);
    }

    void *mapped = nullptr;
    if (vkMapMemory(device, stagingMemory, 0, imageSize, 0, &mapped) != VK_SUCCESS) {
        vkDestroyBuffer(device, stagingBuffer, nullptr);
        vkFreeMemory(device, stagingMemory, nullptr);
        return fail("Failed to map texture staging memory");
    }
    std::memcpy(mapped, pixels, static_cast<size_t>(imageSize));
    vkUnmapMemory(device, stagingMemory);

    if (!device_.createImage(width,
                             height,
                             VK_FORMAT_R8G8B8A8_UNORM,
                             VK_IMAGE_TILING_OPTIMAL,
                             VK_IMAGE_USAGE_TRANSFER_DST_BIT | VK_IMAGE_USAGE_SAMPLED_BIT,
                             VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT,
                             texture.image,
                             texture.imageMemory,
                             &errorMessage)) {
        vkDestroyBuffer(device, stagingBuffer, nullptr);
        vkFreeMemory(device, stagingMemory, nullptr);
        return fail(errorMessage);
    }

    if (!transitionImageLayout(texture.image, VK_IMAGE_LAYOUT_UNDEFINED, VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL) ||
        !copyBufferToImage(stagingBuffer, texture.image, width, height) ||
        !transitionImageLayout(texture.image,
                               VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL,
                               VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL)) {
        vkDestroyBuffer(device, stagingBuffer, nullptr);
        vkFreeMemory(device, stagingMemory, nullptr);
        destroyTexture(slot);
        return false;
    }

    vkDestroyBuffer(device, stagingBuffer, nullptr);
    vkFreeMemory(device, stagingMemory, nullptr);

    VkImageViewCreateInfo viewInfo{VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO};
    viewInfo.image = texture.image;
    viewInfo.viewType = VK_IMAGE_VIEW_TYPE_2D;
    viewInfo.format = VK_FORMAT_R8G8B8A8_UNORM;
    viewInfo.subresourceRange.aspectMask = VK_IMAGE_ASPECT_COLOR_BIT;
    viewInfo.subresourceRange.baseMipLevel = 0;
    viewInfo.subresourceRange.levelCount = 1;
    viewInfo.subresourceRange.baseArrayLayer = 0;
    viewInfo.subresourceRange.layerCount = 1;

    if (vkCreateImageView(device, &viewInfo, nullptr, &texture.imageView) != VK_SUCCESS) {
        destroyTexture(slot);
        return fail("Failed to create texture image view");
    }

    VkSamplerCreateInfo samplerInfo{VK_STRUCTURE_TYPE_SAMPLER_CREATE_INFO};
    samplerInfo.magFilter = VK_FILTER_LINEAR;
    samplerInfo.minFilter = VK_FILTER_LINEAR;
    samplerInfo.addressModeU = VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_EDGE;
    samplerInfo.addressModeV = VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_EDGE;
    samplerInfo.addressModeW = VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_EDGE;
    samplerInfo.anisotropyEnable = VK_FALSE;
    samplerInfo.maxAnisotropy = 1.0f;
    samplerInfo.borderColor = VK_BORDER_COLOR_INT_OPAQUE_BLACK;
    samplerInfo.unnormalizedCoordinates = VK_FALSE;
    samplerInfo.compareEnable = VK_FALSE;
    samplerInfo.compareOp = VK_COMPARE_OP_ALWAYS;
    samplerInfo.mipmapMode = VK_SAMPLER_MIPMAP_MODE_LINEAR;

    if (vkCreateSampler(device, &samplerInfo, nullptr, &texture.sampler) != VK_SUCCESS) {
        destroyTexture(slot);
        return fail("Failed to create texture sampler");
    }

    texture.width = width;
    texture.height = height;
    return true;
}

void VulkanRenderer::destroyTexture(uint32_t slot) {
    if (slot >= textures_.size()) {
        return;
    }
    VkDevice device = device_.device();
    if (device == VK_NULL_HANDLE) {
        return;
    }

    TextureResource &texture = textures_[slot];
    if (texture.sampler != VK_NULL_HANDLE) {
        vkDestroySampler(device, texture.sampler, nullptr);
        texture.sampler = VK_NULL_HANDLE;
    }
    if (texture.imageView != VK_NULL_HANDLE) {
        vkDestroyImageView(device, texture.imageView, nullptr);
        texture.imageView = VK_NULL_HANDLE;
    }
    if (texture.image != VK_NULL_HANDLE) {
        vkDestroyImage(device, texture.image, nullptr);
        texture.image = VK_NULL_HANDLE;
    }
    if (texture.imageMemory != VK_NULL_HANDLE) {
        vkFreeMemory(device, texture.imageMemory, nullptr);
        texture.imageMemory = VK_NULL_HANDLE;
    }
    texture.width = 0;
    texture.height = 0;
}

void VulkanRenderer::destroyAllTextures() {
    for (uint32_t slot = 0; slot < textures_.size(); ++slot) {
        destroyTexture(slot);
    }
}

bool VulkanRenderer::transitionImageLayout(VkImage image,
                                           VkImageLayout oldLayout,
                                           VkImageLayout newLayout) {
    std::string errorMessage;
    VkCommandBuffer commandBuffer = frameResources_.beginOneTimeCommands(device_.device(), &errorMessage);
    if (commandBuffer == VK_NULL_HANDLE) {
        return fail(errorMessage);
    }

    VkImageMemoryBarrier barrier{VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER};
    barrier.oldLayout = oldLayout;
    barrier.newLayout = newLayout;
    barrier.srcQueueFamilyIndex = VK_QUEUE_FAMILY_IGNORED;
    barrier.dstQueueFamilyIndex = VK_QUEUE_FAMILY_IGNORED;
    barrier.image = image;
    barrier.subresourceRange.aspectMask = VK_IMAGE_ASPECT_COLOR_BIT;
    barrier.subresourceRange.baseMipLevel = 0;
    barrier.subresourceRange.levelCount = 1;
    barrier.subresourceRange.baseArrayLayer = 0;
    barrier.subresourceRange.layerCount = 1;

    VkPipelineStageFlags sourceStage = VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT;
    VkPipelineStageFlags destinationStage = VK_PIPELINE_STAGE_TRANSFER_BIT;

    if (oldLayout == VK_IMAGE_LAYOUT_UNDEFINED && newLayout == VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL) {
        barrier.srcAccessMask = 0;
        barrier.dstAccessMask = VK_ACCESS_TRANSFER_WRITE_BIT;
        sourceStage = VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT;
        destinationStage = VK_PIPELINE_STAGE_TRANSFER_BIT;
    } else if (oldLayout == VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL &&
               newLayout == VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL) {
        barrier.srcAccessMask = VK_ACCESS_TRANSFER_WRITE_BIT;
        barrier.dstAccessMask = VK_ACCESS_SHADER_READ_BIT;
        sourceStage = VK_PIPELINE_STAGE_TRANSFER_BIT;
        destinationStage = VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT;
    }

    vkCmdPipelineBarrier(
            commandBuffer,
            sourceStage, destinationStage,
            0,
            0, nullptr,
            0, nullptr,
            1, &barrier);

    if (!frameResources_.endOneTimeCommands(device_.device(),
                                            device_.graphicsQueue(),
                                            commandBuffer,
                                            &errorMessage)) {
        return fail(errorMessage);
    }

    return true;
}

bool VulkanRenderer::copyBufferToImage(VkBuffer buffer, VkImage image, uint32_t width, uint32_t height) {
    std::string errorMessage;
    VkCommandBuffer commandBuffer = frameResources_.beginOneTimeCommands(device_.device(), &errorMessage);
    if (commandBuffer == VK_NULL_HANDLE) {
        return fail(errorMessage);
    }

    VkBufferImageCopy region{};
    region.bufferOffset = 0;
    region.bufferRowLength = 0;
    region.bufferImageHeight = 0;
    region.imageSubresource.aspectMask = VK_IMAGE_ASPECT_COLOR_BIT;
    region.imageSubresource.mipLevel = 0;
    region.imageSubresource.baseArrayLayer = 0;
    region.imageSubresource.layerCount = 1;
    region.imageOffset = {0, 0, 0};
    region.imageExtent = {width, height, 1};

    vkCmdCopyBufferToImage(
            commandBuffer,
            buffer,
            image,
            VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL,
            1,
            &region);

    if (!frameResources_.endOneTimeCommands(device_.device(),
                                            device_.graphicsQueue(),
                                            commandBuffer,
                                            &errorMessage)) {
        return fail(errorMessage);
    }

    return true;
}

std::vector<uint8_t> VulkanRenderer::loadAssetBytes(const char *assetName) const {
    std::vector<uint8_t> bytes;
    if (assetManager_ == nullptr) {
        return bytes;
    }

    AAsset *asset = AAssetManager_open(assetManager_, assetName, AASSET_MODE_STREAMING);
    if (asset == nullptr) {
        return bytes;
    }

    size_t length = static_cast<size_t>(AAsset_getLength(asset));
    bytes.resize(length);
    int readBytes = AAsset_read(asset, bytes.data(), length);
    AAsset_close(asset);

    if (readBytes <= 0) {
        bytes.clear();
        return bytes;
    }

    if (static_cast<size_t>(readBytes) != length) {
        bytes.resize(static_cast<size_t>(readBytes));
    }
    return bytes;
}

void VulkanRenderer::setError(const char *message) {
    {
        std::lock_guard<std::mutex> lock(errorMutex_);
        lastError_ = message;
    }
    LOGE("%s", message);
}

void VulkanRenderer::setError(const std::string &message) {
    setError(message.c_str());
}

bool VulkanRenderer::fail(const std::string &message) {
    setError(message);
    return false;
}

void VulkanRenderer::clearError() {
    std::lock_guard<std::mutex> lock(errorMutex_);
    lastError_.clear();
}

void VulkanRenderer::clearPendingSceneSnapshot() {
    std::atomic_store_explicit(&pendingSceneSnapshot_,
                               std::shared_ptr<const SubmittedSceneSnapshot>{},
                               std::memory_order_release);
}

void VulkanRenderer::publishPendingSceneSnapshot(std::shared_ptr<const SubmittedSceneSnapshot> sceneSnapshot) {
    std::atomic_exchange_explicit(&pendingSceneSnapshot_,
                                  std::move(sceneSnapshot),
                                  std::memory_order_acq_rel);
}

std::shared_ptr<const VulkanRenderer::SubmittedSceneSnapshot> VulkanRenderer::consumePendingSceneSnapshot() {
    return std::atomic_exchange_explicit(&pendingSceneSnapshot_,
                                         std::shared_ptr<const SubmittedSceneSnapshot>{},
                                         std::memory_order_acq_rel);
}

} // namespace nativelib::render

namespace nativelib::vulkan_render_engine {

bool init(JNIEnv *env, jobject activity, jobject surface, jobject assetManager) {
    return gRenderer.init(env, activity, surface, assetManager);
}

void resize(int32_t width, int32_t height) {
    gRenderer.resize(static_cast<jint>(width), static_cast<jint>(height));
}

int32_t uploadTexture(JNIEnv *env,
                      jstring debugName,
                      int32_t width,
                      int32_t height,
                      jobject rgbaBuffer,
                      int32_t targetSlot) {
    if (targetSlot == kAutoTextureSlot) {
        return gRenderer.uploadTextureRgba(env,
                                           debugName,
                                           static_cast<jint>(width),
                                           static_cast<jint>(height),
                                           rgbaBuffer);
    }
    return gRenderer.uploadTextureRgbaToSlot(env,
                                             debugName,
                                             static_cast<jint>(width),
                                             static_cast<jint>(height),
                                             rgbaBuffer,
                                             static_cast<jint>(targetSlot));
}

bool submitSceneFrame(JNIEnv *env,
                      int32_t payloadVersion,
                      jintArray previousTextureSlots,
                      jintArray previousBlendModes,
                      jintArray previousLayers,
                      jintArray previousRenderOrders,
                      jintArray previousScissorXs,
                      jintArray previousScissorYs,
                      jintArray previousScissorWidths,
                      jintArray previousScissorHeights,
                      jfloatArray previousTextureUs,
                      jfloatArray previousTextureVs,
                      jfloatArray previousTextureWidthUvs,
                      jfloatArray previousTextureHeightUvs,
                      jfloatArray previousXs,
                      jfloatArray previousYs,
                      jfloatArray previousZs,
                      jfloatArray previousScaleXs,
                      jfloatArray previousScaleYs,
                      jfloatArray previousRotationDegs,
                      jfloatArray previousAnimationStates,
                      jintArray currentTextureSlots,
                      jintArray currentBlendModes,
                      jintArray currentLayers,
                      jintArray currentRenderOrders,
                      jintArray currentScissorXs,
                      jintArray currentScissorYs,
                      jintArray currentScissorWidths,
                      jintArray currentScissorHeights,
                      jfloatArray currentTextureUs,
                      jfloatArray currentTextureVs,
                      jfloatArray currentTextureWidthUvs,
                      jfloatArray currentTextureHeightUvs,
                      jfloatArray currentXs,
                      jfloatArray currentYs,
                      jfloatArray currentZs,
                      jfloatArray currentScaleXs,
                      jfloatArray currentScaleYs,
                      jfloatArray currentRotationDegs,
                      jfloatArray currentAnimationStates,
                      float previousCameraX,
                      float previousCameraY,
                      float previousCameraZoom,
                      float previousCameraRotationDeg,
                      float currentCameraX,
                      float currentCameraY,
                      float currentCameraZoom,
                      float currentCameraRotationDeg,
                      float interpolationAlpha) {
    return gRenderer.submitReadOnlySceneFrame(env,
                                              static_cast<jint>(payloadVersion),
                                              previousTextureSlots,
                                              previousBlendModes,
                                              previousLayers,
                                              previousRenderOrders,
                                              previousScissorXs,
                                              previousScissorYs,
                                              previousScissorWidths,
                                              previousScissorHeights,
                                              previousTextureUs,
                                              previousTextureVs,
                                              previousTextureWidthUvs,
                                              previousTextureHeightUvs,
                                              previousXs,
                                              previousYs,
                                              previousZs,
                                              previousScaleXs,
                                              previousScaleYs,
                                              previousRotationDegs,
                                              previousAnimationStates,
                                              currentTextureSlots,
                                              currentBlendModes,
                                              currentLayers,
                                              currentRenderOrders,
                                              currentScissorXs,
                                              currentScissorYs,
                                              currentScissorWidths,
                                              currentScissorHeights,
                                              currentTextureUs,
                                              currentTextureVs,
                                              currentTextureWidthUvs,
                                              currentTextureHeightUvs,
                                              currentXs,
                                              currentYs,
                                              currentZs,
                                              currentScaleXs,
                                              currentScaleYs,
                                              currentRotationDegs,
                                              currentAnimationStates,
                                              previousCameraX,
                                              previousCameraY,
                                              previousCameraZoom,
                                              previousCameraRotationDeg,
                                              currentCameraX,
                                              currentCameraY,
                                              currentCameraZoom,
                                              currentCameraRotationDeg,
                                              interpolationAlpha) == JNI_TRUE;
}

bool renderFrame() {
    return gRenderer.renderFrame();
}

void cleanup() {
    gRenderer.cleanup();
}

std::string lastError() {
    return gRenderer.lastError();
}

std::string textureUploadStatus() {
    return gRenderer.textureUploadStatus();
}

std::string swapchainStatus() {
    return gRenderer.swapchainStatus();
}

bool releaseTexture(int32_t slot) {
    return gRenderer.releaseTexture(static_cast<jint>(slot)) == JNI_TRUE;
}

void clearTextureCache() {
    gRenderer.clearTextureCache();
}

} // namespace nativelib::vulkan_render_engine
