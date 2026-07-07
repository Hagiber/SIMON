package com.hsw.simonapp.engine.audio;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;
import android.util.Log;

import java.util.Arrays;
import java.util.Objects;

public final class GeneratedBounceAudioPlayer implements AudioPlayer {

    private static final String TAG = "BounceAudioPlayer";
    private static final int SAMPLE_RATE_HZ = 44_100;
    private static final int BOUNCE_SOUND_DURATION_MILLIS = 120;
    private static final int RELEASE_DELAY_MILLIS = 20;
    private static final double START_FREQUENCY_HZ = 820.0d;
    private static final double END_FREQUENCY_HZ = 260.0d;
    private static final double MAX_VOLUME = 0.42d;

    private final short[] bounceSamples;

    public GeneratedBounceAudioPlayer() {
        this(createBounceSamples());
    }

    GeneratedBounceAudioPlayer(short[] bounceSamples) {
        this.bounceSamples = Arrays.copyOf(Objects.requireNonNull(bounceSamples, "bounceSamples"),
                bounceSamples.length);
    }

    @Override
    public void play(AudioEvent event) {
        AudioEvent nonNullEvent = Objects.requireNonNull(event, "event");
        if (nonNullEvent.getType() != AudioEventType.COLLISION_SOUND) {
            return;
        }

        Thread audioThread = new Thread(() -> playBounceSamples(bounceSamples), "CollisionBounceAudio");
        audioThread.setDaemon(true);
        audioThread.start();
    }

    static short[] createBounceSamples() {
        int sampleCount = SAMPLE_RATE_HZ * BOUNCE_SOUND_DURATION_MILLIS / 1000;
        short[] samples = new short[sampleCount];
        double phase = 0.0d;

        for (int sampleIndex = 0; sampleIndex < sampleCount; sampleIndex++) {
            double progress = sampleIndex / (double) Math.max(1, sampleCount - 1);
            double frequency = START_FREQUENCY_HZ + (END_FREQUENCY_HZ - START_FREQUENCY_HZ) * progress;
            phase += 2.0d * Math.PI * frequency / SAMPLE_RATE_HZ;

            double attack = Math.min(1.0d, progress * 45.0d);
            double decay = Math.exp(-7.8d * progress);
            double primaryTone = Math.sin(phase);
            double harmonic = 0.28d * Math.sin(phase * 2.0d);
            double sample = (primaryTone + harmonic) * attack * decay * MAX_VOLUME;
            samples[sampleIndex] = clampToPcm16(sample);
        }

        return samples;
    }

    private static void playBounceSamples(short[] samples) {
        AudioTrack audioTrack = null;
        try {
            audioTrack = createAudioTrack(samples.length);
            if (audioTrack.getState() != AudioTrack.STATE_INITIALIZED) {
                Log.w(TAG, "AudioTrack was not initialized for collision sound");
                return;
            }

            int writtenSamples = audioTrack.write(samples, 0, samples.length);
            if (writtenSamples <= 0) {
                Log.w(TAG, "Failed to write collision sound samples: " + writtenSamples);
                return;
            }

            audioTrack.play();
            Thread.sleep(BOUNCE_SOUND_DURATION_MILLIS + RELEASE_DELAY_MILLIS);
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
        } catch (RuntimeException runtimeException) {
            Log.w(TAG, "Failed to play collision sound", runtimeException);
        } finally {
            releaseAudioTrack(audioTrack);
        }
    }

    private static AudioTrack createAudioTrack(int sampleCount) {
        return new AudioTrack.Builder()
                .setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build())
                .setAudioFormat(new AudioFormat.Builder()
                        .setSampleRate(SAMPLE_RATE_HZ)
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build())
                .setTransferMode(AudioTrack.MODE_STATIC)
                .setBufferSizeInBytes(sampleCount * Short.BYTES)
                .build();
    }

    private static void releaseAudioTrack(AudioTrack audioTrack) {
        if (audioTrack == null) {
            return;
        }

        try {
            audioTrack.stop();
        } catch (IllegalStateException ignored) {
            // The track may never have reached PLAYSTATE_PLAYING.
        } finally {
            audioTrack.release();
        }
    }

    private static short clampToPcm16(double sample) {
        double clampedSample = Math.max(-1.0d, Math.min(1.0d, sample));
        return (short) Math.round(clampedSample * Short.MAX_VALUE);
    }
}
