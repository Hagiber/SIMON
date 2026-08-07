package com.hsw.simonapp.engine.audio;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class GeneratedSimonButtonAudioPlayerTest {

    @Test
    public void createToneSamples_generatesShortAudibleWaveform() {
        short[] samples = GeneratedSimonButtonAudioPlayer.createToneSamples(310.0d);

        assertEquals(7_938, samples.length);
        assertTrue(hasAudibleSample(samples));
    }

    @Test
    public void clampVolume_limitsVolumeToPlayableRange() {
        assertEquals(0.0f, GeneratedSimonButtonAudioPlayer.clampVolume(-0.2f), 0.0001f);
        assertEquals(0.45f, GeneratedSimonButtonAudioPlayer.clampVolume(0.45f), 0.0001f);
        assertEquals(1.0f, GeneratedSimonButtonAudioPlayer.clampVolume(1.4f), 0.0001f);
        assertEquals(1.0f, GeneratedSimonButtonAudioPlayer.clampVolume(Float.NaN), 0.0001f);
    }

    private static boolean hasAudibleSample(short[] samples) {
        for (short sample : samples) {
            if (Math.abs(sample) > 1_000) {
                return true;
            }
        }
        return false;
    }
}
