package com.edss;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

/**
 * Fails the build if any module leaks into another's internals. This is the
 * enforcement point for the "no cross-module Java calls" rule — everything
 * must go through {@code spi} interfaces or events.
 */
class ModuleStructureTest {

    @Test
    void verifyModularStructure() {
        ApplicationModules modules = ApplicationModules.of(EdssApplication.class);
        modules.verify();
    }
}
