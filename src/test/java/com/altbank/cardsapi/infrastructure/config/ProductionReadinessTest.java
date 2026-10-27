package com.altbank.cardsapi.infrastructure.config;

import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ProductionReadinessTest {

    @Test
    void blocksWhenBothAdaptersAreSimulator() {
        List<String> problems = ProductionReadiness.problems("simulator", "simulator");

        Assertions.assertEquals(2, problems.size());
        Assertions.assertTrue(problems.get(0).contains("processadora"));
        Assertions.assertTrue(problems.get(1).contains("transportadora"));
    }

    @Test
    void blocksWhenAdapterIsNullOrBlank() {
        Assertions.assertEquals(2, ProductionReadiness.problems(null, " ").size());
    }

    @Test
    void treatsSimulatorCaseInsensitively() {
        Assertions.assertEquals(2, ProductionReadiness.problems("SIMULATOR", "Simulator").size());
    }

    @Test
    void blocksOnlyTheSimulatedSide() {
        List<String> problems = ProductionReadiness.problems("acme-processor", "simulator");

        Assertions.assertEquals(1, problems.size());
        Assertions.assertTrue(problems.get(0).contains("transportadora"));
    }

    @Test
    void readyWhenBothAdaptersAreReal() {
        Assertions.assertTrue(ProductionReadiness.problems("acme-processor", "acme-carrier").isEmpty());
    }
}
