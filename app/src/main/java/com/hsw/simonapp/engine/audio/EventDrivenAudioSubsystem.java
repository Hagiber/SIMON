package com.hsw.simonapp.engine.audio;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class EventDrivenAudioSubsystem implements AudioSubsystem {

    static final long DEFAULT_COLLISION_SOUND_THROTTLE_TICKS = 6L;

    private final AudioEventQueue audioEventQueue;
    private final AudioPlayer audioPlayer;
    private final Map<AudioEventType, Long> throttleTicksByType;
    private final Map<String, Long> lastPlayedTickByThrottleKey = new HashMap<>();

    public EventDrivenAudioSubsystem(AudioEventQueue audioEventQueue, AudioPlayer audioPlayer) {
        this(audioEventQueue, audioPlayer, defaultThrottleTicksByType());
    }

    public EventDrivenAudioSubsystem(AudioEventQueue audioEventQueue,
                                     AudioPlayer audioPlayer,
                                     Map<AudioEventType, Long> throttleTicksByType) {
        this.audioEventQueue = Objects.requireNonNull(audioEventQueue, "audioEventQueue");
        this.audioPlayer = Objects.requireNonNull(audioPlayer, "audioPlayer");
        this.throttleTicksByType = copyThrottleTicksByType(throttleTicksByType);
    }

    @Override
    public void consumePendingEvents() {
        List<AudioEvent> audioEvents = audioEventQueue.drain();
        Set<String> consumedDedupeKeys = new HashSet<>();
        for (AudioEvent audioEvent : audioEvents) {
            if (!consumedDedupeKeys.add(toBatchDedupeKey(audioEvent))) {
                continue;
            }
            if (isThrottled(audioEvent)) {
                continue;
            }

            audioPlayer.play(audioEvent);
            lastPlayedTickByThrottleKey.put(audioEvent.getThrottleKey(), audioEvent.getSourceTickIndex());
        }
    }

    private boolean isThrottled(AudioEvent audioEvent) {
        long throttleTicks = getThrottleTicks(audioEvent.getType());
        if (throttleTicks <= 0L) {
            return false;
        }

        Long lastPlayedTick = lastPlayedTickByThrottleKey.get(audioEvent.getThrottleKey());
        if (lastPlayedTick == null) {
            return false;
        }

        long ticksSinceLastPlay = audioEvent.getSourceTickIndex() - lastPlayedTick;
        return ticksSinceLastPlay >= 0L && ticksSinceLastPlay < throttleTicks;
    }

    private static String toBatchDedupeKey(AudioEvent audioEvent) {
        return audioEvent.getSourceTickIndex() + ":" + audioEvent.getDedupeKey();
    }

    private long getThrottleTicks(AudioEventType audioEventType) {
        Long throttleTicks = throttleTicksByType.get(audioEventType);
        if (throttleTicks == null) {
            return 0L;
        }
        return throttleTicks;
    }

    private static Map<AudioEventType, Long> defaultThrottleTicksByType() {
        Map<AudioEventType, Long> throttleTicks = new EnumMap<>(AudioEventType.class);
        throttleTicks.put(AudioEventType.COLLISION_SOUND, DEFAULT_COLLISION_SOUND_THROTTLE_TICKS);
        return throttleTicks;
    }

    private static Map<AudioEventType, Long> copyThrottleTicksByType(Map<AudioEventType, Long> source) {
        Objects.requireNonNull(source, "throttleTicksByType");
        Map<AudioEventType, Long> copy = new EnumMap<>(AudioEventType.class);
        for (Map.Entry<AudioEventType, Long> entry : source.entrySet()) {
            AudioEventType eventType = Objects.requireNonNull(entry.getKey(), "audioEventType");
            Long throttleTicks = Objects.requireNonNull(entry.getValue(), "throttleTicks");
            if (throttleTicks < 0L) {
                throw new IllegalArgumentException("throttleTicks must be >= 0, got: " + throttleTicks);
            }
            copy.put(eventType, throttleTicks);
        }
        return copy;
    }
}
