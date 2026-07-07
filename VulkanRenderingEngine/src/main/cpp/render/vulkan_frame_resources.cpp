#include "vulkan_frame_resources.h"

#include <algorithm>
#include <cstdint>

namespace nativelib::render {
namespace {

bool fail(std::string *errorMessage, const char *message) {
    if (errorMessage != nullptr) {
        *errorMessage = message;
    }
    return false;
}

bool batchScissor(const SpriteDrawBatch &batch, VkExtent2D extent, VkRect2D &scissor) {
    if (extent.width == 0 || extent.height == 0) {
        return false;
    }

    if (!batch.scissor.enabled) {
        scissor.offset = {0, 0};
        scissor.extent = extent;
        return true;
    }

    int64_t left = std::max<int64_t>(0, batch.scissor.x);
    int64_t top = std::max<int64_t>(0, batch.scissor.y);
    int64_t right = std::min<int64_t>(static_cast<int64_t>(extent.width),
                                      static_cast<int64_t>(batch.scissor.x) + batch.scissor.width);
    int64_t bottom = std::min<int64_t>(static_cast<int64_t>(extent.height),
                                       static_cast<int64_t>(batch.scissor.y) + batch.scissor.height);

    if (right <= left || bottom <= top) {
        return false;
    }

    scissor.offset = {static_cast<int32_t>(left), static_cast<int32_t>(top)};
    scissor.extent = {
            static_cast<uint32_t>(right - left),
            static_cast<uint32_t>(bottom - top)
    };
    return true;
}

} // namespace

bool FrameResources::createCommandPool(VkDevice device,
                                       uint32_t graphicsQueueFamilyIndex,
                                       std::string *errorMessage) {
    VkCommandPoolCreateInfo poolInfo{VK_STRUCTURE_TYPE_COMMAND_POOL_CREATE_INFO};
    poolInfo.flags = VK_COMMAND_POOL_CREATE_RESET_COMMAND_BUFFER_BIT;
    poolInfo.queueFamilyIndex = graphicsQueueFamilyIndex;

    if (vkCreateCommandPool(device, &poolInfo, nullptr, &commandPool_) != VK_SUCCESS) {
        return fail(errorMessage, "Failed to create command pool");
    }

    return true;
}

bool FrameResources::createSyncObjects(VkDevice device, std::string *errorMessage) {
    VkSemaphoreCreateInfo semaphoreInfo{VK_STRUCTURE_TYPE_SEMAPHORE_CREATE_INFO};
    VkFenceCreateInfo fenceInfo{VK_STRUCTURE_TYPE_FENCE_CREATE_INFO};
    fenceInfo.flags = VK_FENCE_CREATE_SIGNALED_BIT;

    imageAvailableSemaphores_.resize(kMaxFramesInFlight, VK_NULL_HANDLE);
    renderFinishedSemaphores_.resize(kMaxFramesInFlight, VK_NULL_HANDLE);
    inFlightFences_.resize(kMaxFramesInFlight, VK_NULL_HANDLE);
    for (uint32_t frameIndex = 0; frameIndex < kMaxFramesInFlight; ++frameIndex) {
        if (vkCreateSemaphore(device,
                              &semaphoreInfo,
                              nullptr,
                              &imageAvailableSemaphores_[frameIndex]) != VK_SUCCESS ||
            vkCreateSemaphore(device,
                              &semaphoreInfo,
                              nullptr,
                              &renderFinishedSemaphores_[frameIndex]) != VK_SUCCESS ||
            vkCreateFence(device,
                          &fenceInfo,
                          nullptr,
                          &inFlightFences_[frameIndex]) != VK_SUCCESS) {
            return fail(errorMessage, "Failed to create frame sync objects");
        }
    }

    return true;
}

bool FrameResources::createFramebuffers(VkDevice device,
                                        VkRenderPass renderPass,
                                        const std::vector<VkImageView> &imageViews,
                                        VkExtent2D extent,
                                        std::string *errorMessage) {
    framebuffers_.resize(imageViews.size());
    for (size_t i = 0; i < imageViews.size(); ++i) {
        VkImageView attachments[] = {imageViews[i]};

        VkFramebufferCreateInfo framebufferInfo{VK_STRUCTURE_TYPE_FRAMEBUFFER_CREATE_INFO};
        framebufferInfo.renderPass = renderPass;
        framebufferInfo.attachmentCount = 1;
        framebufferInfo.pAttachments = attachments;
        framebufferInfo.width = extent.width;
        framebufferInfo.height = extent.height;
        framebufferInfo.layers = 1;

        if (vkCreateFramebuffer(device, &framebufferInfo, nullptr, &framebuffers_[i]) != VK_SUCCESS) {
            return fail(errorMessage, "Failed to create framebuffer");
        }
    }

    return true;
}

bool FrameResources::allocateCommandBuffers(VkDevice device,
                                            size_t swapchainImageCount,
                                            std::string *errorMessage) {
    swapchainImageCount_ = swapchainImageCount;
    imagesInFlight_.assign(swapchainImageCount_, VK_NULL_HANDLE);
    commandBuffers_.resize(swapchainImageCount_ * kMaxFramesInFlight);
    VkCommandBufferAllocateInfo allocInfo{VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO};
    allocInfo.commandPool = commandPool_;
    allocInfo.level = VK_COMMAND_BUFFER_LEVEL_PRIMARY;
    allocInfo.commandBufferCount = static_cast<uint32_t>(commandBuffers_.size());

    if (vkAllocateCommandBuffers(device, &allocInfo, commandBuffers_.data()) != VK_SUCCESS) {
        return fail(errorMessage, "Failed to allocate command buffers");
    }

    return true;
}

bool FrameResources::recordCommandBuffers(VkDevice device,
                                          VkRenderPass renderPass,
                                          VkExtent2D extent,
                                          const Pipeline &pipeline,
                                          VkBuffer vertexBuffer,
                                          const std::vector<VkBuffer> &instanceBuffers,
                                          const std::vector<VkDescriptorSet> &cameraDescriptorSets,
                                          const std::vector<VkDescriptorSet> &textureDescriptorSets,
                                          const std::vector<TextureResource> &textures,
                                          const std::vector<SpriteDrawBatch> &spriteDrawBatches,
                                          std::string *errorMessage) {
    if (commandBuffers_.empty()) {
        return fail(errorMessage, "No command buffers allocated");
    }
    if (swapchainImageCount_ == 0 || framebuffers_.size() != swapchainImageCount_) {
        return fail(errorMessage, "Swapchain frame resources are inconsistent");
    }
    if (instanceBuffers.size() < kMaxFramesInFlight ||
        cameraDescriptorSets.size() < kMaxFramesInFlight) {
        return fail(errorMessage, "Per-frame draw resources are incomplete");
    }

    for (uint32_t frameIndex = 0; frameIndex < kMaxFramesInFlight; ++frameIndex) {
        for (size_t imageIndex = 0; imageIndex < swapchainImageCount_; ++imageIndex) {
            const size_t commandBufferIndex =
                    static_cast<size_t>(frameIndex) * swapchainImageCount_ + imageIndex;
            VkCommandBuffer commandBuffer = commandBuffers_[commandBufferIndex];
            if (vkResetCommandBuffer(commandBuffer, 0) != VK_SUCCESS) {
                return fail(errorMessage, "Failed to reset command buffer");
            }
            VkCommandBufferBeginInfo beginInfo{VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO};
            if (vkBeginCommandBuffer(commandBuffer, &beginInfo) != VK_SUCCESS) {
                return fail(errorMessage, "Failed to begin command buffer");
            }

            VkClearValue clearColor = {{{0.05f, 0.05f, 0.1f, 1.0f}}};

            VkRenderPassBeginInfo renderPassBeginInfo{VK_STRUCTURE_TYPE_RENDER_PASS_BEGIN_INFO};
            renderPassBeginInfo.renderPass = renderPass;
            renderPassBeginInfo.framebuffer = framebuffers_[imageIndex];
            renderPassBeginInfo.renderArea.offset = {0, 0};
            renderPassBeginInfo.renderArea.extent = extent;
            renderPassBeginInfo.clearValueCount = 1;
            renderPassBeginInfo.pClearValues = &clearColor;

            vkCmdBeginRenderPass(commandBuffer, &renderPassBeginInfo, VK_SUBPASS_CONTENTS_INLINE);

            VkBuffer vertexBuffers[] = {vertexBuffer};
            VkDeviceSize offsets[] = {0};
            vkCmdBindVertexBuffers(commandBuffer, 0, 1, vertexBuffers, offsets);

            VkBuffer instanceBuffer = instanceBuffers[frameIndex];
            if (instanceBuffer != VK_NULL_HANDLE && !spriteDrawBatches.empty()) {
                VkBuffer buffers[] = {vertexBuffer, instanceBuffer};
                VkDeviceSize bufferOffsets[] = {0, 0};
                vkCmdBindVertexBuffers(commandBuffer, 0, 2, buffers, bufferOffsets);
            }

            VkPipeline boundPipeline = VK_NULL_HANDLE;
            for (const SpriteDrawBatch &batch : spriteDrawBatches) {
                uint32_t slot = batch.textureSlot;
                if (batch.instanceCount == 0) {
                    continue;
                }
                if (slot >= textureDescriptorSets.size()) {
                    continue;
                }
                if (slot >= textures.size() ||
                    textures[slot].imageView == VK_NULL_HANDLE ||
                    textures[slot].sampler == VK_NULL_HANDLE) {
                    continue;
                }

                VkRect2D scissor{};
                if (!batchScissor(batch, extent, scissor)) {
                    continue;
                }

                VkPipeline batchPipeline = pipeline.handle(batch.blendMode);
                if (batchPipeline == VK_NULL_HANDLE) {
                    return fail(errorMessage, "Sprite batch blend pipeline is not available");
                }
                if (boundPipeline != batchPipeline) {
                    vkCmdBindPipeline(commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, batchPipeline);
                    boundPipeline = batchPipeline;
                }

                VkDescriptorSet cameraDescriptorSet = cameraDescriptorSets[frameIndex];
                VkDescriptorSet sets[] = {cameraDescriptorSet, textureDescriptorSets[slot]};
                vkCmdBindDescriptorSets(commandBuffer,
                                        VK_PIPELINE_BIND_POINT_GRAPHICS,
                                        pipeline.layout(),
                                        0,
                                        2,
                                        sets,
                                        0,
                                        nullptr);

                vkCmdSetScissor(commandBuffer, 0, 1, &scissor);
                vkCmdDraw(commandBuffer, 6, batch.instanceCount, 0, batch.firstInstance);
            }
            vkCmdEndRenderPass(commandBuffer);

            if (vkEndCommandBuffer(commandBuffer) != VK_SUCCESS) {
                return fail(errorMessage, "Failed to record command buffer");
            }
        }
    }

    return true;
}

VkCommandBuffer FrameResources::beginOneTimeCommands(VkDevice device, std::string *errorMessage) const {
    VkCommandBufferAllocateInfo allocInfo{VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO};
    allocInfo.level = VK_COMMAND_BUFFER_LEVEL_PRIMARY;
    allocInfo.commandPool = commandPool_;
    allocInfo.commandBufferCount = 1;

    VkCommandBuffer commandBuffer = VK_NULL_HANDLE;
    if (vkAllocateCommandBuffers(device, &allocInfo, &commandBuffer) != VK_SUCCESS) {
        fail(errorMessage, "Failed to allocate one-time command buffer");
        return VK_NULL_HANDLE;
    }

    VkCommandBufferBeginInfo beginInfo{VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO};
    beginInfo.flags = VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT;
    if (vkBeginCommandBuffer(commandBuffer, &beginInfo) != VK_SUCCESS) {
        vkFreeCommandBuffers(device, commandPool_, 1, &commandBuffer);
        fail(errorMessage, "Failed to begin one-time command buffer");
        return VK_NULL_HANDLE;
    }

    return commandBuffer;
}

bool FrameResources::endOneTimeCommands(VkDevice device,
                                        VkQueue graphicsQueue,
                                        VkCommandBuffer commandBuffer,
                                        std::string *errorMessage) const {
    if (commandBuffer == VK_NULL_HANDLE) {
        return fail(errorMessage, "One-time command buffer is null");
    }

    if (vkEndCommandBuffer(commandBuffer) != VK_SUCCESS) {
        vkFreeCommandBuffers(device, commandPool_, 1, &commandBuffer);
        return fail(errorMessage, "Failed to end one-time command buffer");
    }

    VkSubmitInfo submitInfo{VK_STRUCTURE_TYPE_SUBMIT_INFO};
    submitInfo.commandBufferCount = 1;
    submitInfo.pCommandBuffers = &commandBuffer;

    if (vkQueueSubmit(graphicsQueue, 1, &submitInfo, VK_NULL_HANDLE) != VK_SUCCESS) {
        vkFreeCommandBuffers(device, commandPool_, 1, &commandBuffer);
        return fail(errorMessage, "Failed to submit one-time command buffer");
    }
    vkQueueWaitIdle(graphicsQueue);

    vkFreeCommandBuffers(device, commandPool_, 1, &commandBuffer);
    return true;
}

void FrameResources::clearSwapchainResources(VkDevice device) {
    if (device == VK_NULL_HANDLE) {
        return;
    }

    for (auto framebuffer : framebuffers_) {
        if (framebuffer != VK_NULL_HANDLE) {
            vkDestroyFramebuffer(device, framebuffer, nullptr);
        }
    }
    framebuffers_.clear();

    if (commandPool_ != VK_NULL_HANDLE && !commandBuffers_.empty()) {
        vkFreeCommandBuffers(device,
                             commandPool_,
                             static_cast<uint32_t>(commandBuffers_.size()),
                             commandBuffers_.data());
    }
    commandBuffers_.clear();
    imagesInFlight_.clear();
    swapchainImageCount_ = 0;
}

void FrameResources::destroy(VkDevice device) {
    clearSwapchainResources(device);

    if (device == VK_NULL_HANDLE) {
        return;
    }

    for (VkSemaphore semaphore : imageAvailableSemaphores_) {
        if (semaphore != VK_NULL_HANDLE) {
            vkDestroySemaphore(device, semaphore, nullptr);
        }
    }
    imageAvailableSemaphores_.clear();

    for (VkSemaphore semaphore : renderFinishedSemaphores_) {
        if (semaphore != VK_NULL_HANDLE) {
            vkDestroySemaphore(device, semaphore, nullptr);
        }
    }
    renderFinishedSemaphores_.clear();

    for (VkFence fence : inFlightFences_) {
        if (fence != VK_NULL_HANDLE) {
            vkDestroyFence(device, fence, nullptr);
        }
    }
    inFlightFences_.clear();

    if (commandPool_ != VK_NULL_HANDLE) {
        vkDestroyCommandPool(device, commandPool_, nullptr);
        commandPool_ = VK_NULL_HANDLE;
    }
}

VkSemaphore FrameResources::imageAvailableSemaphore(uint32_t frameIndex) const {
    return imageAvailableSemaphores_[frameIndex];
}

VkSemaphore FrameResources::renderFinishedSemaphore(uint32_t frameIndex) const {
    return renderFinishedSemaphores_[frameIndex];
}

VkFence FrameResources::inFlightFence(uint32_t frameIndex) const {
    return inFlightFences_[frameIndex];
}

VkFence FrameResources::imageInFlightFence(uint32_t imageIndex) const {
    return imagesInFlight_[imageIndex];
}

void FrameResources::setImageInFlightFence(uint32_t imageIndex, VkFence fence) {
    imagesInFlight_[imageIndex] = fence;
}

VkCommandBuffer FrameResources::commandBuffer(uint32_t frameIndex, uint32_t imageIndex) const {
    return commandBuffers_[static_cast<size_t>(frameIndex) * swapchainImageCount_ + imageIndex];
}

size_t FrameResources::commandBufferCount() const {
    return commandBuffers_.size();
}

size_t FrameResources::swapchainImageCount() const {
    return swapchainImageCount_;
}

} // namespace nativelib::render
