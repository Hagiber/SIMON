package com.hsw.simonapp.engine.audio;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;
import android.util.Log;

import java.util.Objects;

public final class GeneratedSimonButtonAudioPlayer implements AudioPlayer {

    private static final String TAG = "SimonButtonAudioPlayer";
    private static final int SAMPLE_RATE_HZ = 44_100;
    private static final int TONE_DURATION_MILLIS = 180;
    private static final int RELEASE_DELAY_MILLIS = 20;
    private static final int PLAYBACK_TIMEOUT_MARGIN_MILLIS = 1000;
    private static final double MAX_VOLUME = 0.99d;
    private static final double[] TONE_FREQUENCIES_HZ = {
            310.00d,
            415.30d,
            209.00d,
            252.00d
    };

    private final short[][] toneSamples;
    private final AudioPlayer fallbackAudioPlayer;
    private volatile float volume = 1.0f;

    public GeneratedSimonButtonAudioPlayer(AudioPlayer fallbackAudioPlayer) {
        this(createDefaultToneSamples(), fallbackAudioPlayer);
    }

    GeneratedSimonButtonAudioPlayer(short[][] toneSamples, AudioPlayer fallbackAudioPlayer) {
        this.toneSamples = copyToneSamples(Objects.requireNonNull(toneSamples, "toneSamples"));
        this.fallbackAudioPlayer = Objects.requireNonNull(fallbackAudioPlayer, "fallbackAudioPlayer");
    }

    @Override
    public void play(AudioEvent event) {
        AudioEvent nonNullEvent = Objects.requireNonNull(event, "event");
        if (nonNullEvent.getType() != AudioEventType.BUTTON_TONE) {
            fallbackAudioPlayer.play(nonNullEvent);
            return;
        }

        int toneIndex = nonNullEvent.getToneIndex();
        if (toneIndex < 0 || toneIndex >= toneSamples.length) {
            Log.w(TAG, "Ignoring button tone with unknown tone index: " + toneIndex);
            return;
        }

        short[] samples = toneSamples[toneIndex];
        float playbackVolume = volume;
        Thread audioThread = new Thread(() -> playToneSamples(samples, playbackVolume), "SimonButtonToneAudio");
        audioThread.setDaemon(true);
        audioThread.start();
    }

    public void setVolume(float volume) {
        this.volume = clampVolume(volume);
    }

    public float getVolume() {
        return volume;
    }

    static short[] createToneSamples(double frequencyHz) {
        if (!Double.isFinite(frequencyHz) || frequencyHz <= 0.0d) {
            throw new IllegalArgumentException("frequencyHz must be > 0, got: " + frequencyHz);
        }

        int sampleCount = SAMPLE_RATE_HZ * TONE_DURATION_MILLIS / 1000;
        short[] samples = new short[sampleCount];
        double phase = 0.0d;

        for (int sampleIndex = 0; sampleIndex < sampleCount; sampleIndex++) {
            double progress = sampleIndex / (double) Math.max(1, sampleCount - 1);
            phase += 2.0d * Math.PI * frequencyHz / SAMPLE_RATE_HZ;

            double attack = Math.min(1.0d, progress * 32.0d);
            double release = Math.min(1.0d, (1.0d - progress) * 12.0d);
            double envelope = attack * release;
            double primaryTone = Math.sin(phase);
            double softHarmonic = 0.14d * Math.sin(phase * 2.0d);
            double sample = (primaryTone + softHarmonic) * envelope * MAX_VOLUME;
            samples[sampleIndex] = clampToPcm16(sample);
        }

        return samples;
    }

    private static short[][] createDefaultToneSamples() {
        short[][] samples = new short[TONE_FREQUENCIES_HZ.length][];
        for (int index = 0; index < TONE_FREQUENCIES_HZ.length; index++) {
            samples[index] = createToneSamples(TONE_FREQUENCIES_HZ[index]);
        }
        return samples;
    }

    private static short[][] copyToneSamples(short[][] source) {
        short[][] copy = new short[source.length][];
        for (int index = 0; index < source.length; index++) {
            copy[index] = Objects.requireNonNull(source[index], "toneSamples[" + index + "]").clone();
        }
        return copy;
    }

    static float clampVolume(float volume) {
        if (!Float.isFinite(volume)) {
            return 1.0f;
        }
        return Math.max(0.0f, Math.min(1.0f, volume));
    }

    private static void playToneSamples(short[] samples, float volume) {
        AudioTrack audioTrack = null;
        try {
            audioTrack = createAudioTrack(samples.length);
            if (audioTrack.getState() != AudioTrack.STATE_INITIALIZED) {
                Log.w(TAG, "AudioTrack was not initialized for button tone");
                return;
            }

            audioTrack.setVolume(clampVolume(volume));
            audioTrack.play();
            int writtenSamples = audioTrack.write(samples,
                    0,
                    samples.length,
                    AudioTrack.WRITE_BLOCKING);
            if (writtenSamples <= 0) {
                Log.w(TAG, "Failed to write button tone samples: " + writtenSamples);
                return;
            }

            waitUntilPlaybackFinishes(audioTrack, samples.length);
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
        } catch (RuntimeException runtimeException) {
            Log.w(TAG, "Failed to play button tone", runtimeException);
        } finally {
            releaseAudioTrack(audioTrack);
        }
    }

    private static AudioTrack createAudioTrack(int sampleCount) {
        int minBufferSizeBytes = AudioTrack.getMinBufferSize(SAMPLE_RATE_HZ,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT);
        if (minBufferSizeBytes <= 0) {
            minBufferSizeBytes = sampleCount * Short.BYTES;
        }
        int bufferSizeBytes = Math.max(minBufferSizeBytes, sampleCount * Short.BYTES);

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
                .setTransferMode(AudioTrack.MODE_STREAM)
                .setBufferSizeInBytes(bufferSizeBytes)
                .build();
    }

    private static void waitUntilPlaybackFinishes(AudioTrack audioTrack, int sampleCount)
            throws InterruptedException {
        long timeoutAtMillis = System.currentTimeMillis()
                + TONE_DURATION_MILLIS
                + PLAYBACK_TIMEOUT_MARGIN_MILLIS;
        while (audioTrack.getPlaybackHeadPosition() < sampleCount
                && System.currentTimeMillis() < timeoutAtMillis) {
            Thread.sleep(5L);
        }
        Thread.sleep(RELEASE_DELAY_MILLIS);
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
