#include "../render/vulkan_render_engine.h"

#include <string>

namespace engine = nativelib::vulkan_render_engine;

extern "C" JNIEXPORT jboolean JNICALL
Java_com_hsw_vulkanrenderingengine_bridge_NativeLibBridge_initVulkan(JNIEnv *env,
                                                          jobject /* this */,
                                                          jobject activity,
                                                          jobject surface,
                                                          jobject assetManager) {
    return engine::init(env, activity, surface, assetManager) ? JNI_TRUE : JNI_FALSE;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_hsw_vulkanrenderingengine_bridge_NativeLibBridge_renderFrame(JNIEnv * /* env */, jobject /* this */) {
    return engine::renderFrame() ? JNI_TRUE : JNI_FALSE;
}

extern "C" JNIEXPORT void JNICALL
Java_com_hsw_vulkanrenderingengine_bridge_NativeLibBridge_resize(JNIEnv * /* env */, jobject /* this */, jint width, jint height) {
    engine::resize(width, height);
}

extern "C" JNIEXPORT void JNICALL
Java_com_hsw_vulkanrenderingengine_bridge_NativeLibBridge_cleanupVulkan(JNIEnv * /* env */, jobject /* this */) {
    engine::cleanup();
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_hsw_vulkanrenderingengine_bridge_NativeLibBridge_getLastVulkanError(JNIEnv *env, jobject /* this */) {
    std::string lastError = engine::lastError();
    return env->NewStringUTF(lastError.c_str());
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_hsw_vulkanrenderingengine_bridge_NativeLibBridge_getTextureUploadStatus(JNIEnv *env,
                                                                                  jobject /* this */) {
    std::string status = engine::textureUploadStatus();
    return env->NewStringUTF(status.c_str());
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_hsw_vulkanrenderingengine_bridge_NativeLibBridge_getSwapchainStatus(JNIEnv *env,
                                                                              jobject /* this */) {
    std::string status = engine::swapchainStatus();
    return env->NewStringUTF(status.c_str());
}

extern "C" JNIEXPORT jint JNICALL
Java_com_hsw_vulkanrenderingengine_bridge_NativeLibBridge_uploadTextureRgba(JNIEnv *env,
                                                                 jobject /* this */,
                                                                 jstring debugName,
                                                                 jint width,
                                                                 jint height,
                                                                 jobject rgbaBuffer) {
    return engine::uploadTexture(env, debugName, width, height, rgbaBuffer, engine::kAutoTextureSlot);
}

extern "C" JNIEXPORT jint JNICALL
Java_com_hsw_vulkanrenderingengine_bridge_NativeLibBridge_uploadTextureRgbaToSlot(JNIEnv *env,
                                                                       jobject /* this */,
                                                                       jstring debugName,
                                                                       jint width,
                                                                       jint height,
                                                                       jobject rgbaBuffer,
                                                                       jint slot) {
    return engine::uploadTexture(env, debugName, width, height, rgbaBuffer, slot);
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_hsw_vulkanrenderingengine_bridge_NativeLibBridge_releaseTexture(JNIEnv * /* env */,
                                                              jobject /* this */,
                                                              jint slot) {
    return engine::releaseTexture(slot) ? JNI_TRUE : JNI_FALSE;
}

extern "C" JNIEXPORT void JNICALL
Java_com_hsw_vulkanrenderingengine_bridge_NativeLibBridge_clearTextureCache(JNIEnv * /* env */, jobject /* this */) {
    engine::clearTextureCache();
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_hsw_vulkanrenderingengine_bridge_NativeLibBridge_submitReadOnlySceneFrameNative(JNIEnv *env,
                                                                              jobject /* this */,
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
    return engine::submitSceneFrame(env,
                                    payloadVersion,
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
                                    interpolationAlpha) ? JNI_TRUE : JNI_FALSE;
}

