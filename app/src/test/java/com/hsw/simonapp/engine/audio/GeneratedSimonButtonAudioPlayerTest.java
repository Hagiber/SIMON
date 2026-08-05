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

    private static boolean hasAudibleSample(short[] samples) {
        for (short sample : samples) {
            if (Math.abs(sample) > 1_000) {
                return true;
            }
        }
        return false;
    }
}
