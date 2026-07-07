package com.hsw.simonapp.engine.audio;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class GeneratedBounceAudioPlayerTest {

    @Test
    public void createBounceSamples_generatesShortAudibleWaveform() {
        short[] samples = GeneratedBounceAudioPlayer.createBounceSamples();

        assertEquals(5_292, samples.length);
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
