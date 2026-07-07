package com.hsw.vulkanrenderingengine.bridge;

import static org.junit.Assert.assertFalse;

import org.junit.Test;

import java.lang.reflect.Modifier;

public class NativeBridgeBoundaryTest {

    @Test
    public void jniImplementationClassesStayPackagePrivate() {
        assertFalse("NativeLibBridge should not be part of the public host API",
                Modifier.isPublic(NativeLibBridge.class.getModifiers()));
        assertFalse("JniNativeHostBridge should not be part of the public host API",
                Modifier.isPublic(JniNativeHostBridge.class.getModifiers()));
    }
}
