#include "vulkan_pipeline.h"

#include <array>
#include <cstddef>

#include "vulkan_types.h"

namespace nativelib::render {
namespace {

bool fail(std::string *errorMessage, const char *message) {
    if (errorMessage != nullptr) {
        *errorMessage = message;
    }
    return false;
}

std::array<nativelib::BlendMode, nativelib::kBlendModeCount> blendModes() {
    return {
            nativelib::BlendMode::Alpha,
            nativelib::BlendMode::Additive,
            nativelib::BlendMode::Multiply
    };
}

std::size_t blendModeIndex(nativelib::BlendMode blendMode) {
    return static_cast<std::size_t>(blendMode);
}

VkPipelineColorBlendAttachmentState colorBlendAttachmentFor(nativelib::BlendMode blendMode) {
    VkPipelineColorBlendAttachmentState attachment{};
    attachment.colorWriteMask = VK_COLOR_COMPONENT_R_BIT | VK_COLOR_COMPONENT_G_BIT |
                                VK_COLOR_COMPONENT_B_BIT | VK_COLOR_COMPONENT_A_BIT;
    attachment.blendEnable = VK_TRUE;
    attachment.colorBlendOp = VK_BLEND_OP_ADD;
    attachment.alphaBlendOp = VK_BLEND_OP_ADD;

    // Sprite textures are normalized to premultiplied RGBA before atlas upload.
    switch (blendMode) {
        case nativelib::BlendMode::Alpha:
            attachment.srcColorBlendFactor = VK_BLEND_FACTOR_ONE;
            attachment.dstColorBlendFactor = VK_BLEND_FACTOR_ONE_MINUS_SRC_ALPHA;
            attachment.srcAlphaBlendFactor = VK_BLEND_FACTOR_ONE;
            attachment.dstAlphaBlendFactor = VK_BLEND_FACTOR_ONE_MINUS_SRC_ALPHA;
            break;
        case nativelib::BlendMode::Additive:
            attachment.srcColorBlendFactor = VK_BLEND_FACTOR_ONE;
            attachment.dstColorBlendFactor = VK_BLEND_FACTOR_ONE;
            attachment.srcAlphaBlendFactor = VK_BLEND_FACTOR_ONE;
            attachment.dstAlphaBlendFactor = VK_BLEND_FACTOR_ONE;
            break;
        case nativelib::BlendMode::Multiply:
            attachment.srcColorBlendFactor = VK_BLEND_FACTOR_DST_COLOR;
            attachment.dstColorBlendFactor = VK_BLEND_FACTOR_ONE_MINUS_SRC_ALPHA;
            attachment.srcAlphaBlendFactor = VK_BLEND_FACTOR_ZERO;
            attachment.dstAlphaBlendFactor = VK_BLEND_FACTOR_ONE;
            break;
    }

    return attachment;
}

} // namespace

bool Pipeline::createShaderModule(VkDevice device,
                                  const std::vector<uint8_t> &code,
                                  VkShaderModule &module,
                                  std::string *errorMessage) const {
    if (code.empty() || code.size() % 4 != 0) {
        return fail(errorMessage, "Invalid SPIR-V bytecode");
    }

    VkShaderModuleCreateInfo createInfo{VK_STRUCTURE_TYPE_SHADER_MODULE_CREATE_INFO};
    createInfo.codeSize = code.size();
    createInfo.pCode = reinterpret_cast<const uint32_t *>(code.data());

    if (vkCreateShaderModule(device, &createInfo, nullptr, &module) != VK_SUCCESS) {
        return fail(errorMessage, "Failed to create shader module");
    }

    return true;
}

bool Pipeline::create(VkDevice device,
                      VkExtent2D extent,
                      VkRenderPass renderPass,
                      VkDescriptorSetLayout cameraDescriptorSetLayout,
                      VkDescriptorSetLayout textureDescriptorSetLayout,
                      const std::vector<uint8_t> &vertexShaderCode,
                      const std::vector<uint8_t> &fragmentShaderCode,
                      std::string *errorMessage) {
    VkShaderModule vertModule = VK_NULL_HANDLE;
    VkShaderModule fragModule = VK_NULL_HANDLE;
    if (!createShaderModule(device, vertexShaderCode, vertModule, errorMessage) ||
        !createShaderModule(device, fragmentShaderCode, fragModule, errorMessage)) {
        if (vertModule != VK_NULL_HANDLE) {
            vkDestroyShaderModule(device, vertModule, nullptr);
        }
        if (fragModule != VK_NULL_HANDLE) {
            vkDestroyShaderModule(device, fragModule, nullptr);
        }
        return false;
    }

    VkPipelineShaderStageCreateInfo vertStage{VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO};
    vertStage.stage = VK_SHADER_STAGE_VERTEX_BIT;
    vertStage.module = vertModule;
    vertStage.pName = "main";

    VkPipelineShaderStageCreateInfo fragStage{VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO};
    fragStage.stage = VK_SHADER_STAGE_FRAGMENT_BIT;
    fragStage.module = fragModule;
    fragStage.pName = "main";

    VkPipelineShaderStageCreateInfo stages[] = {vertStage, fragStage};

    std::array<VkVertexInputBindingDescription, 2> bindings{};
    bindings[0].binding = 0;
    bindings[0].stride = sizeof(Vertex);
    bindings[0].inputRate = VK_VERTEX_INPUT_RATE_VERTEX;
    bindings[1].binding = 1;
    bindings[1].stride = sizeof(SpriteInstance);
    bindings[1].inputRate = VK_VERTEX_INPUT_RATE_INSTANCE;

    std::array<VkVertexInputAttributeDescription, 7> attributes{};
    attributes[0].location = 0;
    attributes[0].binding = 0;
    attributes[0].format = VK_FORMAT_R32G32_SFLOAT;
    attributes[0].offset = offsetof(Vertex, pos);

    attributes[1].location = 1;
    attributes[1].binding = 0;
    attributes[1].format = VK_FORMAT_R32G32_SFLOAT;
    attributes[1].offset = offsetof(Vertex, uv);

    attributes[2].location = 2;
    attributes[2].binding = 0;
    attributes[2].format = VK_FORMAT_R32G32B32A32_SFLOAT;
    attributes[2].offset = offsetof(Vertex, color);

    attributes[3].location = 3;
    attributes[3].binding = 1;
    attributes[3].format = VK_FORMAT_R32G32B32A32_SFLOAT;
    attributes[3].offset = offsetof(SpriteInstance, model);

    attributes[4].location = 4;
    attributes[4].binding = 1;
    attributes[4].format = VK_FORMAT_R32G32B32A32_SFLOAT;
    attributes[4].offset = offsetof(SpriteInstance, model) + sizeof(float) * 4u;

    attributes[5].location = 5;
    attributes[5].binding = 1;
    attributes[5].format = VK_FORMAT_R32G32B32A32_SFLOAT;
    attributes[5].offset = offsetof(SpriteInstance, uvRect);

    attributes[6].location = 6;
    attributes[6].binding = 1;
    attributes[6].format = VK_FORMAT_R32G32B32A32_SFLOAT;
    attributes[6].offset = offsetof(SpriteInstance, color);

    VkPipelineVertexInputStateCreateInfo vertexInputInfo{VK_STRUCTURE_TYPE_PIPELINE_VERTEX_INPUT_STATE_CREATE_INFO};
    vertexInputInfo.vertexBindingDescriptionCount = static_cast<uint32_t>(bindings.size());
    vertexInputInfo.pVertexBindingDescriptions = bindings.data();
    vertexInputInfo.vertexAttributeDescriptionCount = static_cast<uint32_t>(attributes.size());
    vertexInputInfo.pVertexAttributeDescriptions = attributes.data();

    VkPipelineInputAssemblyStateCreateInfo inputAssembly{VK_STRUCTURE_TYPE_PIPELINE_INPUT_ASSEMBLY_STATE_CREATE_INFO};
    inputAssembly.topology = VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST;

    VkViewport viewport{};
    viewport.width = static_cast<float>(extent.width);
    viewport.height = static_cast<float>(extent.height);
    viewport.minDepth = 0.0f;
    viewport.maxDepth = 1.0f;

    VkRect2D scissor{};
    scissor.extent = extent;

    VkPipelineViewportStateCreateInfo viewportState{VK_STRUCTURE_TYPE_PIPELINE_VIEWPORT_STATE_CREATE_INFO};
    viewportState.viewportCount = 1;
    viewportState.pViewports = &viewport;
    viewportState.scissorCount = 1;
    viewportState.pScissors = &scissor;

    std::array<VkDynamicState, 1> dynamicStates = {
            VK_DYNAMIC_STATE_SCISSOR
    };
    VkPipelineDynamicStateCreateInfo dynamicState{VK_STRUCTURE_TYPE_PIPELINE_DYNAMIC_STATE_CREATE_INFO};
    dynamicState.dynamicStateCount = static_cast<uint32_t>(dynamicStates.size());
    dynamicState.pDynamicStates = dynamicStates.data();

    VkPipelineRasterizationStateCreateInfo rasterizer{VK_STRUCTURE_TYPE_PIPELINE_RASTERIZATION_STATE_CREATE_INFO};
    rasterizer.polygonMode = VK_POLYGON_MODE_FILL;
    rasterizer.lineWidth = 1.0f;
    // Android Vulkan framebuffers can flip winding relative to our sprite vertices.
    rasterizer.cullMode = VK_CULL_MODE_NONE;
    rasterizer.frontFace = VK_FRONT_FACE_COUNTER_CLOCKWISE;

    VkPipelineMultisampleStateCreateInfo multisampling{VK_STRUCTURE_TYPE_PIPELINE_MULTISAMPLE_STATE_CREATE_INFO};
    multisampling.rasterizationSamples = VK_SAMPLE_COUNT_1_BIT;

    VkPipelineColorBlendAttachmentState colorBlendAttachment =
            colorBlendAttachmentFor(nativelib::BlendMode::Alpha);

    VkPipelineColorBlendStateCreateInfo colorBlending{VK_STRUCTURE_TYPE_PIPELINE_COLOR_BLEND_STATE_CREATE_INFO};
    colorBlending.attachmentCount = 1;
    colorBlending.pAttachments = &colorBlendAttachment;

    std::array<VkDescriptorSetLayout, 2> descriptorLayouts = {
            cameraDescriptorSetLayout,
            textureDescriptorSetLayout
    };

    VkPipelineLayoutCreateInfo pipelineLayoutInfo{VK_STRUCTURE_TYPE_PIPELINE_LAYOUT_CREATE_INFO};
    pipelineLayoutInfo.setLayoutCount = static_cast<uint32_t>(descriptorLayouts.size());
    pipelineLayoutInfo.pSetLayouts = descriptorLayouts.data();

    if (vkCreatePipelineLayout(device, &pipelineLayoutInfo, nullptr, &pipelineLayout_) != VK_SUCCESS) {
        vkDestroyShaderModule(device, vertModule, nullptr);
        vkDestroyShaderModule(device, fragModule, nullptr);
        return fail(errorMessage, "Failed to create pipeline layout");
    }

    VkGraphicsPipelineCreateInfo pipelineInfo{VK_STRUCTURE_TYPE_GRAPHICS_PIPELINE_CREATE_INFO};
    pipelineInfo.stageCount = 2;
    pipelineInfo.pStages = stages;
    pipelineInfo.pVertexInputState = &vertexInputInfo;
    pipelineInfo.pInputAssemblyState = &inputAssembly;
    pipelineInfo.pViewportState = &viewportState;
    pipelineInfo.pRasterizationState = &rasterizer;
    pipelineInfo.pMultisampleState = &multisampling;
    pipelineInfo.pColorBlendState = &colorBlending;
    pipelineInfo.pDynamicState = &dynamicState;
    pipelineInfo.layout = pipelineLayout_;
    pipelineInfo.renderPass = renderPass;
    pipelineInfo.subpass = 0;

    for (nativelib::BlendMode blendMode : blendModes()) {
        colorBlendAttachment = colorBlendAttachmentFor(blendMode);
        if (vkCreateGraphicsPipelines(device,
                                      VK_NULL_HANDLE,
                                      1,
                                      &pipelineInfo,
                                      nullptr,
                                      &graphicsPipelines_[blendModeIndex(blendMode)]) != VK_SUCCESS) {
            destroy(device);
            vkDestroyShaderModule(device, vertModule, nullptr);
            vkDestroyShaderModule(device, fragModule, nullptr);
            return fail(errorMessage, "Failed to create graphics pipeline");
        }
    }

    vkDestroyShaderModule(device, vertModule, nullptr);
    vkDestroyShaderModule(device, fragModule, nullptr);
    return true;
}

void Pipeline::destroy(VkDevice device) {
    if (device == VK_NULL_HANDLE) {
        return;
    }

    for (VkPipeline &graphicsPipeline : graphicsPipelines_) {
        if (graphicsPipeline != VK_NULL_HANDLE) {
            vkDestroyPipeline(device, graphicsPipeline, nullptr);
            graphicsPipeline = VK_NULL_HANDLE;
        }
    }

    if (pipelineLayout_ != VK_NULL_HANDLE) {
        vkDestroyPipelineLayout(device, pipelineLayout_, nullptr);
        pipelineLayout_ = VK_NULL_HANDLE;
    }
}

VkPipeline Pipeline::handle() const {
    return handle(nativelib::BlendMode::Alpha);
}

VkPipeline Pipeline::handle(nativelib::BlendMode blendMode) const {
    std::size_t index = blendModeIndex(blendMode);
    if (index >= graphicsPipelines_.size()) {
        return VK_NULL_HANDLE;
    }
    return graphicsPipelines_[index];
}

VkPipelineLayout Pipeline::layout() const {
    return pipelineLayout_;
}

} // namespace nativelib::render
