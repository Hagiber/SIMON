#pragma once

#include <array>
#include <cstdint>
#include <string>
#include <vector>

#include <vulkan/vulkan.h>

#include "../core/render_scene.h"

namespace nativelib::render {

class Pipeline {
public:
    bool create(VkDevice device,
                VkExtent2D extent,
                VkRenderPass renderPass,
                VkDescriptorSetLayout cameraDescriptorSetLayout,
                VkDescriptorSetLayout textureDescriptorSetLayout,
                const std::vector<uint8_t> &vertexShaderCode,
                const std::vector<uint8_t> &fragmentShaderCode,
                std::string *errorMessage);
    void destroy(VkDevice device);

    VkPipeline handle() const;
    VkPipeline handle(nativelib::BlendMode blendMode) const;
    VkPipelineLayout layout() const;

private:
    bool createShaderModule(VkDevice device,
                            const std::vector<uint8_t> &code,
                            VkShaderModule &module,
                            std::string *errorMessage) const;

    VkPipelineLayout pipelineLayout_ = VK_NULL_HANDLE;
    std::array<VkPipeline, nativelib::kBlendModeCount> graphicsPipelines_{};
};

} // namespace nativelib::render
