package com.hsw.etalonapp;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

public class HostModuleBoundaryTest {

    @Test
    public void hostSourcesDoNotImportEngineInternalsOrRendererBridge() throws IOException {
        Path hostSources = resolveHostSources();
        assertTrue("Missing host source directory: " + hostSources, Files.isDirectory(hostSources));

        try (Stream<Path> javaFiles = Files.walk(hostSources)) {
            for (Path javaFile : (Iterable<Path>) javaFiles
                    .filter(path -> path.toString().endsWith(".java"))
                    .filter(path -> !isInsideEnginePackage(hostSources, path))::iterator) {
                String source = new String(Files.readAllBytes(javaFile), StandardCharsets.UTF_8);
                assertFalse(javaFile + " imports engine internals",
                        source.contains("com.hsw.etalonapp.engine.assets.")
                                || source.contains("com.hsw.etalonapp.engine.audio.")
                                || source.contains("com.hsw.etalonapp.engine.bridge.")
                                || source.contains("com.hsw.etalonapp.engine.input.")
                                || source.contains("com.hsw.etalonapp.engine.loop.")
                                || source.contains("com.hsw.etalonapp.engine.render.")
                                || source.contains("com.hsw.etalonapp.engine.scene.")
                                || source.contains("com.hsw.vulkanrenderingengine.bridge."));
                assertFalse(javaFile + " retains host-side render/update engine packages",
                        source.contains("com.hsw.etalonapp.loop.")
                                || source.contains("com.hsw.etalonapp.data.")
                                || source.contains("com.hsw.etalonapp.audio.")
                                || source.contains("com.hsw.etalonapp.bridge."));
            }
        }
    }

    private static boolean isInsideEnginePackage(Path hostSources, Path javaFile) {
        Path relative = hostSources.relativize(javaFile);
        return relative.getNameCount() > 0 && "engine".equals(relative.getName(0).toString());
    }

    private static Path resolveHostSources() {
        Path userDir = Paths.get(System.getProperty("user.dir"));
        Path moduleRelative = userDir.resolve("src/main/java/com/hsw/etalonapp");
        if (Files.isDirectory(moduleRelative)) {
            return moduleRelative;
        }
        return userDir.resolve("app/src/main/java/com/hsw/etalonapp");
    }
}
