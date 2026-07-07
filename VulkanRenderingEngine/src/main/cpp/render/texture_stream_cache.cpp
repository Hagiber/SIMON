#include "texture_stream_cache.h"

#include <functional>
#include <limits>

#include "../core/render_scene.h"

namespace nativelib::render {
namespace {

bool fail(std::string *errorMessage, const char *message) {
    if (errorMessage != nullptr) {
        *errorMessage = message;
    }
    return false;
}

size_t combineHash(size_t seed, size_t value) {
    return seed ^ (value + 0x9e3779b9u + (seed << 6u) + (seed >> 2u));
}

} // namespace

void TextureStreamCache::reset() {
    cachedSlots_.clear();
    slots_.clear();
    pendingUploads_.clear();
}

bool TextureStreamCache::enqueueAuto(const std::string &debugName,
                                     uint32_t width,
                                     uint32_t height,
                                     const uint8_t *pixels,
                                     size_t byteCount,
                                     EnqueueResult *result,
                                     std::string *errorMessage) {
    return enqueue(debugName, width, height, pixels, byteCount, false, 0, result, errorMessage);
}

bool TextureStreamCache::enqueueToSlot(const std::string &debugName,
                                       uint32_t width,
                                       uint32_t height,
                                       const uint8_t *pixels,
                                       size_t byteCount,
                                       uint32_t requestedSlot,
                                       EnqueueResult *result,
                                       std::string *errorMessage) {
    return enqueue(debugName,
                   width,
                   height,
                   pixels,
                   byteCount,
                   true,
                   requestedSlot,
                   result,
                   errorMessage);
}

bool TextureStreamCache::processPendingUploads(SpriteBatcher &spriteBatcher,
                                               uint32_t maxUploadCount,
                                               uint32_t *processedCount,
                                               std::string *errorMessage) {
    uint32_t processed = 0;
    while (!pendingUploads_.empty() &&
           (maxUploadCount == 0 || processed < maxUploadCount)) {
        PendingUpload upload = std::move(pendingUploads_.front());
        pendingUploads_.pop_front();

        if (upload.slot >= slots_.size() ||
            !slots_[upload.slot].reserved ||
            !(slots_[upload.slot].key == upload.key)) {
            continue;
        }

        if (!spriteBatcher.setTexture(upload.slot,
                                      upload.pixels.data(),
                                      upload.key.width,
                                      upload.key.height,
                                      errorMessage)) {
            if (processedCount != nullptr) {
                *processedCount = processed;
            }
            return false;
        }
        ++processed;
    }

    if (processedCount != nullptr) {
        *processedCount = processed;
    }
    return true;
}

bool TextureStreamCache::releaseSlot(uint32_t slot,
                                     SpriteBatcher &spriteBatcher,
                                     std::string *errorMessage) {
    removeCacheEntriesForSlot(slot);
    removePendingUploadForSlot(slot);
    if (slot < slots_.size()) {
        slots_[slot] = SlotEntry{};
    }
    return spriteBatcher.clearTexture(slot, errorMessage);
}

void TextureStreamCache::clear(SpriteBatcher &spriteBatcher) {
    reset();
    spriteBatcher.clearTextures();
}

bool TextureStreamCache::hasPendingUploads() const {
    return !pendingUploads_.empty();
}

size_t TextureStreamCache::pendingUploadCount() const {
    return pendingUploads_.size();
}

size_t TextureStreamCache::reservedSlotCount() const {
    size_t reservedCount = 0;
    for (const SlotEntry &slot : slots_) {
        if (slot.reserved) {
            ++reservedCount;
        }
    }
    return reservedCount;
}

bool TextureStreamCache::TextureCacheKey::operator==(const TextureCacheKey &other) const {
    return width == other.width &&
           height == other.height &&
           contentHash == other.contentHash &&
           debugName == other.debugName;
}

size_t TextureStreamCache::TextureCacheKeyHash::operator()(const TextureCacheKey &key) const {
    size_t seed = std::hash<std::string>{}(key.debugName);
    seed = combineHash(seed, std::hash<uint32_t>{}(key.width));
    seed = combineHash(seed, std::hash<uint32_t>{}(key.height));
    seed = combineHash(seed, std::hash<uint64_t>{}(key.contentHash));
    return seed;
}

bool TextureStreamCache::enqueue(const std::string &debugName,
                                 uint32_t width,
                                 uint32_t height,
                                 const uint8_t *pixels,
                                 size_t byteCount,
                                 bool useRequestedSlot,
                                 uint32_t requestedSlot,
                                 EnqueueResult *result,
                                 std::string *errorMessage) {
    if (result == nullptr) {
        return fail(errorMessage, "Texture enqueue result is null");
    }
    *result = EnqueueResult{};

    TextureCacheKey key{};
    if (!makeKey(debugName, width, height, pixels, byteCount, &key, errorMessage)) {
        return false;
    }

    if (!useRequestedSlot) {
        auto cached = cachedSlots_.find(key);
        if (cached != cachedSlots_.end()) {
            result->slot = cached->second;
            result->cacheHit = true;
            return true;
        }
    } else if (requestedSlot < slots_.size() &&
               slots_[requestedSlot].reserved &&
               slots_[requestedSlot].key == key) {
        cachedSlots_[key] = requestedSlot;
        result->slot = requestedSlot;
        result->cacheHit = true;
        return true;
    }

    uint32_t slot = useRequestedSlot ? requestedSlot : findAvailableSlot();
    if (slot > nativelib::kMaxSceneTextureSlot) {
        return fail(errorMessage,
                    useRequestedSlot
                    ? "Texture slot exceeds max scene texture slot"
                    : "No scene texture slot is available within the max scene texture slot");
    }
    ensureSlot(slot);
    removeCacheEntriesForSlot(slot);
    removePendingUploadForSlot(slot);

    slots_[slot].reserved = true;
    slots_[slot].key = key;
    cachedSlots_[key] = slot;

    PendingUpload upload{};
    upload.slot = slot;
    upload.key = key;
    upload.pixels.assign(pixels, pixels + byteCount);
    pendingUploads_.push_back(std::move(upload));

    result->slot = slot;
    result->queued = true;
    return true;
}

uint32_t TextureStreamCache::findAvailableSlot() const {
    for (uint32_t slot = 0; slot < slots_.size(); ++slot) {
        if (!slots_[slot].reserved) {
            return slot;
        }
    }
    return static_cast<uint32_t>(slots_.size());
}

void TextureStreamCache::ensureSlot(uint32_t slot) {
    if (slot >= slots_.size()) {
        slots_.resize(static_cast<size_t>(slot) + 1u);
    }
}

void TextureStreamCache::removePendingUploadForSlot(uint32_t slot) {
    for (auto it = pendingUploads_.begin(); it != pendingUploads_.end();) {
        if (it->slot == slot) {
            it = pendingUploads_.erase(it);
        } else {
            ++it;
        }
    }
}

void TextureStreamCache::removeCacheEntriesForSlot(uint32_t slot) {
    for (auto it = cachedSlots_.begin(); it != cachedSlots_.end();) {
        if (it->second == slot) {
            it = cachedSlots_.erase(it);
        } else {
            ++it;
        }
    }
}

bool TextureStreamCache::makeKey(const std::string &debugName,
                                 uint32_t width,
                                 uint32_t height,
                                 const uint8_t *pixels,
                                 size_t byteCount,
                                 TextureCacheKey *key,
                                 std::string *errorMessage) {
    if (key == nullptr) {
        return fail(errorMessage, "Texture cache key output is null");
    }
    if (pixels == nullptr || width == 0 || height == 0) {
        return fail(errorMessage, "Invalid texture input data");
    }

    uint64_t expectedByteCount = static_cast<uint64_t>(width) *
                                 static_cast<uint64_t>(height) *
                                 4u;
    if (expectedByteCount > std::numeric_limits<size_t>::max()) {
        return fail(errorMessage, "Texture input data is too large");
    }
    if (byteCount != static_cast<size_t>(expectedByteCount)) {
        return fail(errorMessage, "Texture input byte count does not match dimensions");
    }

    key->debugName = debugName;
    key->width = width;
    key->height = height;
    key->contentHash = hashPixels(pixels, byteCount);
    return true;
}

uint64_t TextureStreamCache::hashPixels(const uint8_t *pixels, size_t byteCount) {
    constexpr uint64_t kFnvOffsetBasis = 14695981039346656037ull;
    constexpr uint64_t kFnvPrime = 1099511628211ull;

    uint64_t hash = kFnvOffsetBasis;
    for (size_t i = 0; i < byteCount; ++i) {
        hash ^= static_cast<uint64_t>(pixels[i]);
        hash *= kFnvPrime;
    }
    return hash;
}

} // namespace nativelib::render
