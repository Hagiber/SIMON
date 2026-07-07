package com.hsw.etalonapp.engine.audio;

import android.content.res.AssetManager;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Objects;

public final class AssetBackedBounceAudioPlayer implements AudioPlayer {

    private static final String TAG = "AssetBounceAudioPlayer";
    private static final String[] DEFAULT_ASSET_PATHS = {
            "audio/collision_bounce.wav",
            "audio/collision_bounce.waw"
    };
    private static final int TARGET_STREAM_BUFFER_MILLIS = 120;
    private static final int RELEASE_DELAY_MILLIS = 20;

    private final PcmSound collisionSound;
    private final AudioPlayer fallbackAudioPlayer;

    public AssetBackedBounceAudioPlayer(AssetManager assetManager) {
        this(assetManager, DEFAULT_ASSET_PATHS, new GeneratedBounceAudioPlayer());
    }

    AssetBackedBounceAudioPlayer(AssetManager assetManager,
                                 String[] assetPaths,
                                 AudioPlayer fallbackAudioPlayer) {
        this.fallbackAudioPlayer = Objects.requireNonNull(fallbackAudioPlayer, "fallbackAudioPlayer");
        this.collisionSound = loadFirstAvailableSound(Objects.requireNonNull(assetManager, "assetManager"),
                Objects.requireNonNull(assetPaths, "assetPaths"));
    }

    @Override
    public void play(AudioEvent event) {
        AudioEvent nonNullEvent = Objects.requireNonNull(event, "event");
        if (nonNullEvent.getType() != AudioEventType.COLLISION_SOUND) {
            return;
        }

        if (collisionSound == null) {
            fallbackAudioPlayer.play(nonNullEvent);
            return;
        }

        Thread audioThread = new Thread(() -> playPcmSound(collisionSound), "CollisionAssetBounceAudio");
        audioThread.setDaemon(true);
        audioThread.start();
    }

    private static PcmSound loadFirstAvailableSound(AssetManager assetManager, String[] assetPaths) {
        for (String assetPath : assetPaths) {
            try (InputStream inputStream = assetManager.open(assetPath)) {
                PcmSound loadedSound = parseWavBytes(readAllBytes(inputStream));
                Log.i(TAG, "Loaded collision sound asset: " + assetPath);
                return loadedSound;
            } catch (IOException ioException) {
                Log.d(TAG, "Collision sound asset unavailable at " + assetPath + ": "
                        + ioException.getMessage());
            }
        }

        Log.w(TAG, "No collision WAV asset found; using generated fallback sound. Expected "
                + DEFAULT_ASSET_PATHS[0]);
        return null;
    }

    static PcmSound parseWavBytes(byte[] wavBytes) throws IOException {
        if (wavBytes.length < 12 || !matchesAscii(wavBytes, 0, "RIFF") || !matchesAscii(wavBytes, 8, "WAVE")) {
            throw new IOException("Unsupported WAV header");
        }

        WavFormat wavFormat = null;
        byte[] pcmBytes = null;
        int offset = 12;
        while (offset + 8 <= wavBytes.length) {
            String chunkId = ascii(wavBytes, offset, 4);
            int chunkSize = readLittleEndianInt(wavBytes, offset + 4);
            int chunkDataStart = offset + 8;
            int chunkDataEnd = chunkDataStart + chunkSize;
            if (chunkSize < 0 || chunkDataEnd > wavBytes.length) {
                throw new IOException("Invalid WAV chunk size for " + chunkId);
            }

            if ("fmt ".equals(chunkId)) {
                wavFormat = parseFormatChunk(wavBytes, chunkDataStart, chunkSize);
            } else if ("data".equals(chunkId)) {
                pcmBytes = Arrays.copyOfRange(wavBytes, chunkDataStart, chunkDataEnd);
            }

            offset = chunkDataEnd + chunkSize % 2;
        }

        if (wavFormat == null) {
            throw new IOException("Missing WAV fmt chunk");
        }
        if (pcmBytes == null || pcmBytes.length == 0) {
            throw new IOException("Missing WAV data chunk");
        }
        return new PcmSound(wavFormat.sampleRateHz, wavFormat.channelMask, pcmBytes);
    }

    private static WavFormat parseFormatChunk(byte[] wavBytes, int chunkDataStart, int chunkSize) throws IOException {
        if (chunkSize < 16) {
            throw new IOException("Invalid WAV fmt chunk");
        }

        int audioFormat = readLittleEndianShort(wavBytes, chunkDataStart);
        int channels = readLittleEndianShort(wavBytes, chunkDataStart + 2);
        int sampleRateHz = readLittleEndianInt(wavBytes, chunkDataStart + 4);
        int bitsPerSample = readLittleEndianShort(wavBytes, chunkDataStart + 14);

        if (audioFormat != 1) {
            throw new IOException("Only PCM WAV files are supported, got format " + audioFormat);
        }
        if (bitsPerSample != 16) {
            throw new IOException("Only 16-bit WAV files are supported, got " + bitsPerSample);
        }

        int channelMask;
        if (channels == 1) {
            channelMask = AudioFormat.CHANNEL_OUT_MONO;
        } else if (channels == 2) {
            channelMask = AudioFormat.CHANNEL_OUT_STEREO;
        } else {
            throw new IOException("Only mono/stereo WAV files are supported, got " + channels);
        }

        return new WavFormat(sampleRateHz, channelMask);
    }

    private static void playPcmSound(PcmSound pcmSound) {
        AudioTrack audioTrack = null;
        try {
            int streamBufferSizeBytes = calculateStreamBufferSizeBytes(pcmSound);
            audioTrack = createAudioTrack(pcmSound, streamBufferSizeBytes);
            if (audioTrack.getState() != AudioTrack.STATE_INITIALIZED) {
                Log.w(TAG, "AudioTrack was not initialized for collision WAV asset");
                return;
            }

            audioTrack.play();
            if (!writePcmBytes(audioTrack, pcmSound.getPcmBytes(), streamBufferSizeBytes)) {
                return;
            }
            waitUntilPlaybackFinishes(audioTrack, pcmSound);
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
        } catch (RuntimeException runtimeException) {
            Log.w(TAG, "Failed to play collision WAV asset", runtimeException);
        } finally {
            releaseAudioTrack(audioTrack);
        }
    }

    private static AudioTrack createAudioTrack(PcmSound pcmSound, int bufferSizeBytes) {
        return new AudioTrack.Builder()
                .setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build())
                .setAudioFormat(new AudioFormat.Builder()
                        .setSampleRate(pcmSound.getSampleRateHz())
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setChannelMask(pcmSound.getChannelMask())
                        .build())
                .setTransferMode(AudioTrack.MODE_STREAM)
                .setBufferSizeInBytes(bufferSizeBytes)
                .build();
    }

    private static int calculateStreamBufferSizeBytes(PcmSound pcmSound) {
        int minBufferSizeBytes = AudioTrack.getMinBufferSize(pcmSound.getSampleRateHz(),
                pcmSound.getChannelMask(),
                AudioFormat.ENCODING_PCM_16BIT);
        if (minBufferSizeBytes <= 0) {
            minBufferSizeBytes = pcmSound.getBytesPerFrame() * pcmSound.getSampleRateHz() / 10;
        }

        int targetBufferSizeBytes = pcmSound.getBytesPerFrame()
                * pcmSound.getSampleRateHz()
                * TARGET_STREAM_BUFFER_MILLIS
                / 1000;
        return Math.max(minBufferSizeBytes, targetBufferSizeBytes);
    }

    private static boolean writePcmBytes(AudioTrack audioTrack, byte[] pcmBytes, int streamBufferSizeBytes) {
        int offset = 0;
        while (offset < pcmBytes.length) {
            int bytesToWrite = Math.min(streamBufferSizeBytes, pcmBytes.length - offset);
            int writtenBytes = audioTrack.write(pcmBytes,
                    offset,
                    bytesToWrite,
                    AudioTrack.WRITE_BLOCKING);
            if (writtenBytes <= 0) {
                Log.w(TAG, "Failed to write collision WAV bytes: " + writtenBytes);
                return false;
            }
            offset += writtenBytes;
        }
        return true;
    }

    private static void waitUntilPlaybackFinishes(AudioTrack audioTrack, PcmSound pcmSound)
            throws InterruptedException {
        long timeoutAtMillis = System.currentTimeMillis() + pcmSound.getDurationMillis() + 1000L;
        while (audioTrack.getPlaybackHeadPosition() < pcmSound.getFrameCount()
                && System.currentTimeMillis() < timeoutAtMillis) {
            Thread.sleep(10L);
        }
        Thread.sleep(RELEASE_DELAY_MILLIS);
    }

    private static byte[] readAllBytes(InputStream inputStream) throws IOException {
        byte[] buffer = new byte[4096];
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        int bytesRead;
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, bytesRead);
        }
        return outputStream.toByteArray();
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

    private static boolean matchesAscii(byte[] bytes, int offset, String expected) {
        return ascii(bytes, offset, expected.length()).equals(expected);
    }

    private static String ascii(byte[] bytes, int offset, int length) {
        StringBuilder builder = new StringBuilder(length);
        for (int index = 0; index < length; index++) {
            builder.append((char) bytes[offset + index]);
        }
        return builder.toString();
    }

    private static int readLittleEndianShort(byte[] bytes, int offset) {
        return bytes[offset] & 0xff
                | (bytes[offset + 1] & 0xff) << 8;
    }

    private static int readLittleEndianInt(byte[] bytes, int offset) {
        return bytes[offset] & 0xff
                | (bytes[offset + 1] & 0xff) << 8
                | (bytes[offset + 2] & 0xff) << 16
                | (bytes[offset + 3] & 0xff) << 24;
    }

    static final class PcmSound {
        private final int sampleRateHz;
        private final int channelMask;
        private final byte[] pcmBytes;

        private PcmSound(int sampleRateHz, int channelMask, byte[] pcmBytes) {
            this.sampleRateHz = sampleRateHz;
            this.channelMask = channelMask;
            this.pcmBytes = Arrays.copyOf(Objects.requireNonNull(pcmBytes, "pcmBytes"), pcmBytes.length);
        }

        int getSampleRateHz() {
            return sampleRateHz;
        }

        int getChannelMask() {
            return channelMask;
        }

        byte[] getPcmBytes() {
            return Arrays.copyOf(pcmBytes, pcmBytes.length);
        }

        int getDurationMillis() {
            return (int) Math.ceil(getFrameCount() * 1000.0d / sampleRateHz);
        }

        int getFrameCount() {
            return pcmBytes.length / getBytesPerFrame();
        }

        int getBytesPerFrame() {
            int channels = channelMask == AudioFormat.CHANNEL_OUT_STEREO ? 2 : 1;
            return Short.BYTES * channels;
        }
    }

    private static final class WavFormat {
        private final int sampleRateHz;
        private final int channelMask;

        private WavFormat(int sampleRateHz, int channelMask) {
            this.sampleRateHz = sampleRateHz;
            this.channelMask = channelMask;
        }
    }
}
