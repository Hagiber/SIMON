#pragma once

#include <atomic>
#include <cstddef>
#include <cstdint>
#include <memory>
#include <mutex>
#include <string>
#include <vector>

#include <jni.h>
#include <vulkan/vulkan.h>

#include "../core/render_scene.h"
#include "../platform/android/native_window_handle.h"
#include "sprite_batcher.h"
#include "texture_stream_cache.h"
#include "vulkan_descriptor_resources.h"
#include "vulkan_device.h"
#include "vulkan_frame_resources.h"
#include "vulkan_pipeline.h"
#include "vulkan_render_pass.h"
#include "vulkan_swapchain.h"
#include "vulkan_types.h"

struct AAssetManager;

namespace nativelib::render {

class VulkanRenderer {
public:
    bool init(JNIEnv *env, jobject activityObj, jobject surfaceObj, jobject assetManagerObj);
    bool renderFrame();
    void resize(jint width, jint height);
    void cleanup();
    std::string lastError() const;
    std::string textureUploadStatus();
    std::string swapchainStatus();

    jint uploadTextureRgba(JNIEnv *env, jstring debugName, jint width, jint height, jobject rgbaBuffer);
    jint uploadTextureRgbaToSlot(JNIEnv *env,
                                 jstring debugName,
                                 jint width,
                                 jint height,
                                 jobject rgbaBuffer,
                                 jint slot);
    jboolean releaseTexture(jint slot);
    void clearTextureCache();
    jboolean submitReadOnlySceneFrame(JNIEnv *env,
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
                                      jfloat interpolationAlpha);

private:
    enum class SwapchainStatus {
        Ready,
        Deferred,
        Failed
    };

    struct SubmittedSceneSnapshot {
        std::uint64_t generation = 0;
        nativelib::OrthoCamera camera;
        std::vector<nativelib::RenderSprite> sprites;
    };

    bool initLocked(JNIEnv *env,
                    jobject activityObj,
                    jobject surfaceObj,
                    jobject assetManagerObj);
    void destroyLocked();
    bool createSurface(JNIEnv *env, jobject surfaceObj);
    bool createSwapchainObjects();
    bool initializeSwappyForSwapchain();
    void destroySwappySwapchain();
    JNIEnv *currentJniEnv() const;
    void releaseActivityReference();
    void clearSwapchainResources();
    SwapchainStatus ensureSwapchainReady();
    SwapchainStatus recreateSwapchain();
    bool rememberSurfaceExtent(jint width, jint height);
    VkExtent2D surfaceExtentOr(VkExtent2D fallback) const;
    bool hasDeferredSurfaceExtent() const;
    bool failAndResetDevice(const std::string &message);
    bool waitForDeviceIdle(const char *operation);
    bool createVertexBuffer();
    bool recordCurrentCommandBuffers();
    bool consumePendingSceneSnapshotLocked();
    bool processPendingTextureUploads();
    bool uploadAtlasTexture();
    bool validateTextureUpload(JNIEnv *env,
                               jint width,
                               jint height,
                               jobject rgbaBuffer,
                               uint8_t **pixels,
                               size_t *byteCount);
    std::string readDebugName(JNIEnv *env, jstring debugName) const;

    bool createTextureFromBytes(uint32_t slot, const uint8_t *pixels, uint32_t width, uint32_t height);
    void destroyTexture(uint32_t slot);
    void destroyAllTextures();
    bool transitionImageLayout(VkImage image, VkImageLayout oldLayout, VkImageLayout newLayout);
    bool copyBufferToImage(VkBuffer buffer, VkImage image, uint32_t width, uint32_t height);

    std::vector<uint8_t> loadAssetBytes(const char *assetName) const;
    void setError(const char *message);
    void setError(const std::string &message);
    bool fail(const std::string &message);
    void clearError();
    void clearPendingSceneSnapshot();
    void publishPendingSceneSnapshot(std::shared_ptr<const SubmittedSceneSnapshot> sceneSnapshot);
    std::shared_ptr<const SubmittedSceneSnapshot> consumePendingSceneSnapshot();

    nativelib::platform::android::NativeWindowHandle windowHandle_;
    AAssetManager *assetManager_ = nullptr;
    JavaVM *javaVm_ = nullptr;
    jobject activity_ = nullptr;

    VulkanDevice device_;
    VulkanSwapchain swapchain_;
    RenderPass renderPass_;
    Pipeline pipeline_;
    FrameResources frameResources_;
    DescriptorResources descriptorResources_;
    SpriteBatcher spriteBatcher_;

    VkBuffer vertexBuffer_ = VK_NULL_HANDLE;
    VkDeviceMemory vertexBufferMemory_ = VK_NULL_HANDLE;
    TextureStreamCache textureStreamCache_;
    std::vector<TextureResource> textures_;
    std::vector<nativelib::RenderSprite> sceneSnapshot_;
    nativelib::OrthoCamera sceneCamera_;
    std::shared_ptr<const SubmittedSceneSnapshot> pendingSceneSnapshot_;

    std::atomic<bool> initialized_{false};
    std::atomic<bool> framebufferResized_{false};
    std::atomic<std::uint64_t> surfaceExtent_{0};
    std::atomic<std::uint64_t> sceneGeneration_{0};
    mutable std::mutex resourceMutex_;
    mutable std::mutex errorMutex_;
    std::string lastError_;
    std::uint64_t textureUploadRequests_ = 0;
    std::uint64_t textureUploadsProcessed_ = 0;
    std::uint64_t atlasUploads_ = 0;
    std::uint64_t swapchainRecreates_ = 0;
    uint32_t currentFrameIndex_ = 0;
    bool swappySwapchainInitialized_ = false;
    std::uint64_t swappyRefreshDurationNanos_ = 0;
    std::string lastTextureUploadStatus_ = "idle";
    std::string lastSwapchainStatus_ = "uninitialized";
};

} // namespace nativelib::render
