#pragma once

#include <cstdint>
#include <string>

#include <jni.h>

namespace nativelib::vulkan_render_engine {

constexpr int32_t kAutoTextureSlot = -1;

bool init(JNIEnv *env, jobject activity, jobject surface, jobject assetManager);
void resize(int32_t width, int32_t height);
int32_t uploadTexture(JNIEnv *env,
                      jstring debugName,
                      int32_t width,
                      int32_t height,
                      jobject rgbaBuffer,
                      int32_t targetSlot);
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
                      float interpolationAlpha);
bool renderFrame();
void cleanup();

std::string lastError();
std::string textureUploadStatus();
std::string swapchainStatus();
bool releaseTexture(int32_t slot);
void clearTextureCache();

} // namespace nativelib::vulkan_render_engine
