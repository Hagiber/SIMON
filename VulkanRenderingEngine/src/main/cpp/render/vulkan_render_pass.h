#pragma once

#include <string>

#include <vulkan/vulkan.h>

namespace nativelib::render {

class RenderPass {
public:
    bool create(VkDevice device, VkFormat colorFormat, std::string *errorMessage);
    void destroy(VkDevice device);

    VkRenderPass handle() const;

private:
    VkRenderPass renderPass_ = VK_NULL_HANDLE;
};

} // namespace nativelib::render
