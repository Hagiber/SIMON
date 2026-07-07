package com.hsw.vulkansmokehost;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import com.hsw.vulkanrenderingengine.bridge.NativeHostBridge;

import org.junit.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public class SmokeHostBoundaryTest {

    @Test
    public void smokeRendererUsesReusableAarBridge() throws Exception {
        assertEquals(NativeHostBridge.class,
                SmokeSampleRenderer.class.getDeclaredField("rendererBridge").getType());
    }

    @Test
    public void smokeRendererDoesNotDeclareHostSpecificNativeMethods() {
        for (Method method : SmokeSampleRenderer.class.getDeclaredMethods()) {
            assertFalse("SmokeSampleRenderer must use the reusable AAR bridge, not declare native method: "
                            + method.getName(),
                    Modifier.isNative(method.getModifiers()));
        }
    }
}
