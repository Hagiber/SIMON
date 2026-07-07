package com.hsw.etalonapp.engine.audio;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import android.media.AudioFormat;

import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class AssetBackedBounceAudioPlayerTest {

    @Test
    public void parseWavBytes_acceptsPcm16MonoWav() throws IOException {
        byte[] pcmBytes = new byte[]{0, 0, -24, 3, 24, -4};
        AssetBackedBounceAudioPlayer.PcmSound pcmSound =
                AssetBackedBounceAudioPlayer.parseWavBytes(createPcm16MonoWav(pcmBytes));

        assertEquals(44_100, pcmSound.getSampleRateHz());
        assertEquals(AudioFormat.CHANNEL_OUT_MONO, pcmSound.getChannelMask());
        assertArrayEquals(pcmBytes, pcmSound.getPcmBytes());
        assertEquals(1, pcmSound.getDurationMillis());
    }

    private static byte[] createPcm16MonoWav(byte[] pcmBytes) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        writeAscii(outputStream, "RIFF");
        writeLittleEndianInt(outputStream, 36 + pcmBytes.length);
        writeAscii(outputStream, "WAVE");
        writeAscii(outputStream, "fmt ");
        writeLittleEndianInt(outputStream, 16);
        writeLittleEndianShort(outputStream, 1);
        writeLittleEndianShort(outputStream, 1);
        writeLittleEndianInt(outputStream, 44_100);
        writeLittleEndianInt(outputStream, 44_100 * Short.BYTES);
        writeLittleEndianShort(outputStream, Short.BYTES);
        writeLittleEndianShort(outputStream, 16);
        writeAscii(outputStream, "data");
        writeLittleEndianInt(outputStream, pcmBytes.length);
        outputStream.write(pcmBytes);
        return outputStream.toByteArray();
    }

    private static void writeAscii(ByteArrayOutputStream outputStream, String value) {
        for (int index = 0; index < value.length(); index++) {
            outputStream.write((byte) value.charAt(index));
        }
    }

    private static void writeLittleEndianShort(ByteArrayOutputStream outputStream, int value) {
        outputStream.write(value & 0xff);
        outputStream.write((value >> 8) & 0xff);
    }

    private static void writeLittleEndianInt(ByteArrayOutputStream outputStream, int value) {
        outputStream.write(value & 0xff);
        outputStream.write((value >> 8) & 0xff);
        outputStream.write((value >> 16) & 0xff);
        outputStream.write((value >> 24) & 0xff);
    }
}
