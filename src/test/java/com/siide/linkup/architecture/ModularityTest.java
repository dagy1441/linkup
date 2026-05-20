package com.siide.linkup.architecture;

import com.siide.linkup.LinkupApplication;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

/**
 * Architectural fitness function. Runs Spring Modulith's verification to ensure:
 * <ul>
 *     <li>No feature module reaches into another module's internal packages.</li>
 *     <li>Cross-module communication goes through published interfaces only.</li>
 *     <li>No cyclic dependencies between modules.</li>
 * </ul>
 * This test MUST stay green for every feature added. If it fails, the offender either
 * exports the API correctly or communicates via a domain event.
 */
class ModularityTest {

    private final ApplicationModules modules = ApplicationModules.of(LinkupApplication.class);

    @Test
    void verifies_module_boundaries() {
        modules.verify();
    }

    @Test
    void writes_module_documentation() {
        new Documenter(modules)
                .writeDocumentation()
                .writeIndividualModulesAsPlantUml();
    }
}
