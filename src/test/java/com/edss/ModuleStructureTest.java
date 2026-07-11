package com.edss;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

/**
 * Fails the build if any module leaks into another module's internals.
 * Cross-module usage must go through {@code spi} packages of domain modules
 * or the {@code @NamedInterface}-tagged subpackages of {@code shared} +
 * {@code integrations}.
 */
class ModuleStructureTest {

    @Test
    void verifyModularStructure() {
        ApplicationModules modules = ApplicationModules.of(EdssApplication.class);
        modules.verify();
    }
}
