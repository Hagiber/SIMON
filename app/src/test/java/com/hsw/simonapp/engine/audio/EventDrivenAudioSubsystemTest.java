package com.hsw.simonapp.engine.audio;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class EventDrivenAudioSubsystemTest {

    @Test
    public void consumePendingEvents_deduplicatesAndThrottlesCollisionSounds() {
        AudioEventQueue audioEventQueue = new AudioEventQueue();
        List<AudioEvent> playedEvents = new ArrayList<>();
        EventDrivenAudioSubsystem audioSubsystem = new EventDrivenAudioSubsystem(audioEventQueue,
                event -> playedEvents.add(event));

        audioEventQueue.publish(AudioEvent.collisionSound(10L, 1, 2));
        audioEventQueue.publish(AudioEvent.collisionSound(10L, 2, 1));
        audioEventQueue.publish(AudioEvent.collisionSound(10L, 3, 4));
        audioSubsystem.consumePendingEvents();

        assertEquals(1, playedEvents.size());
        assertEquals("collision_sound:1:2", playedEvents.get(0).getDedupeKey());

        audioEventQueue.publish(AudioEvent.collisionSound(15L, 1, 2));
        audioSubsystem.consumePendingEvents();

        assertEquals(1, playedEvents.size());

        audioEventQueue.publish(AudioEvent.collisionSound(16L, 1, 2));
        audioSubsystem.consumePendingEvents();

        assertEquals(2, playedEvents.size());
    }

    @Test
    public void consumePendingEvents_usesCustomThrottlePolicy() {
        AudioEventQueue audioEventQueue = new AudioEventQueue();
        List<AudioEvent> playedEvents = new ArrayList<>();
        Map<AudioEventType, Long> throttleTicksByType = new EnumMap<>(AudioEventType.class);
        throttleTicksByType.put(AudioEventType.COLLISION_SOUND, 0L);
        EventDrivenAudioSubsystem audioSubsystem = new EventDrivenAudioSubsystem(audioEventQueue,
                event -> playedEvents.add(event),
                throttleTicksByType);

        audioEventQueue.publish(AudioEvent.collisionSound(1L, 1, 2));
        audioEventQueue.publish(AudioEvent.collisionSound(1L, 3, 4));
        audioSubsystem.consumePendingEvents();

        assertEquals(2, playedEvents.size());
    }

    @Test
    public void consumePendingEvents_deduplicatesPerTickBeforeThrottle() {
        AudioEventQueue audioEventQueue = new AudioEventQueue();
        List<AudioEvent> playedEvents = new ArrayList<>();
        EventDrivenAudioSubsystem audioSubsystem = new EventDrivenAudioSubsystem(audioEventQueue,
                event -> playedEvents.add(event));

        audioEventQueue.publish(AudioEvent.collisionSound(10L, 1, 2));
        audioEventQueue.publish(AudioEvent.collisionSound(10L, 2, 1));
        audioEventQueue.publish(AudioEvent.collisionSound(16L, 1, 2));
        audioSubsystem.consumePendingEvents();

        assertEquals(2, playedEvents.size());
        assertEquals(10L, playedEvents.get(0).getSourceTickIndex());
        assertEquals(16L, playedEvents.get(1).getSourceTickIndex());
    }
}
