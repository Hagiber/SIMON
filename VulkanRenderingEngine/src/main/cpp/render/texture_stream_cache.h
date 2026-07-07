#pragma once

#include <cstddef>
#include <cstdint>
#include <deque>
#include <string>
#include <unordered_map>
#include <vector>

#include "sprite_batcher.h"

namespace nativelib::render {

class TextureStreamCache {
public:
    struct EnqueueResult {
        uint32_t slot = 0;
        bool cacheHit = false;
        bool queued = false;
    };

    void reset();

    bool enqueueAuto(const std::string &debugName,
                     uint32_t width,
                     uint32_t height,
                     const uint8_t *pixels,
                     size_t byteCount,
                     EnqueueResult *result,
                     std::string *errorMessage);
    bool enqueueToSlot(const std::string &debugName,
                       uint32_t width,
                       uint32_t height,
                       const uint8_t *pixels,
                       size_t byteCount,
                       uint32_t requestedSlot,
                       EnqueueResult *result,
                       std::string *errorMessage);

    bool processPendingUploads(SpriteBatcher &spriteBatcher,
                               uint32_t maxUploadCount,
                               uint32_t *processedCount,
                               std::string *errorMessage);
    bool releaseSlot(uint32_t slot, SpriteBatcher &spriteBatcher, std::string *errorMessage);
    void clear(SpriteBatcher &spriteBatcher);
    bool hasPendingUploads() const;
    size_t pendingUploadCount() const;
    size_t reservedSlotCount() const;

private:
    struct TextureCacheKey {
        std::string debugName;
        uint32_t width = 0;
        uint32_t height = 0;
        uint64_t contentHash = 0;

        bool operator==(const TextureCacheKey &other) const;
    };

    struct TextureCacheKeyHash {
        size_t operator()(const TextureCacheKey &key) const;
    };

    struct SlotEntry {
        bool reserved = false;
        TextureCacheKey key;
    };

    struct PendingUpload {
        uint32_t slot = 0;
        TextureCacheKey key;
        std::vector<uint8_t> pixels;
    };

    bool enqueue(const std::string &debugName,
                 uint32_t width,
                 uint32_t height,
                 const uint8_t *pixels,
                 size_t byteCount,
                 bool useRequestedSlot,
                 uint32_t requestedSlot,
                 EnqueueResult *result,
                 std::string *errorMessage);
    uint32_t findAvailableSlot() const;
    void ensureSlot(uint32_t slot);
    void removePendingUploadForSlot(uint32_t slot);
    void removeCacheEntriesForSlot(uint32_t slot);

    static bool makeKey(const std::string &debugName,
                        uint32_t width,
                        uint32_t height,
                        const uint8_t *pixels,
                        size_t byteCount,
                        TextureCacheKey *key,
                        std::string *errorMessage);
    static uint64_t hashPixels(const uint8_t *pixels, size_t byteCount);

    std::unordered_map<TextureCacheKey, uint32_t, TextureCacheKeyHash> cachedSlots_;
    std::vector<SlotEntry> slots_;
    std::deque<PendingUpload> pendingUploads_;
};

} // namespace nativelib::render
