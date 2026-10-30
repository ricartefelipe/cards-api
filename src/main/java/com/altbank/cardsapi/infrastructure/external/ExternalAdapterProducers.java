package com.altbank.cardsapi.infrastructure.external;

import com.altbank.cardsapi.application.port.CarrierPort;
import com.altbank.cardsapi.application.port.ProcessorPort;
import com.altbank.cardsapi.infrastructure.config.ProductionReadiness;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Escolhe os adaptadores externos em tempo de <em>execucao</em>, pela configuracao, e nao no build:
 * a mesma imagem final serve producao (adaptadores reais contratados) e o perfil demo
 * (simuladores). Em producao com simulador, o ProductionReadinessGuard recusa iniciar antes de
 * qualquer uso; sem adaptador real implementado, os portos "nao configurados" falham explicitamente.
 */
@ApplicationScoped
public class ExternalAdapterProducers {

    @Produces
    @ApplicationScoped
    CarrierPort carrierPort(@ConfigProperty(name = "cards.carrier.adapter") String adapter,
                            InMemoryCarrierAdapter simulator) {
        return ProductionReadiness.isSimulator(adapter) ? simulator : new UnconfiguredCarrierPort();
    }

    @Produces
    @ApplicationScoped
    ProcessorPort processorPort(@ConfigProperty(name = "cards.processor.adapter") String adapter,
                                InMemoryProcessorSimulator simulator) {
        return ProductionReadiness.isSimulator(adapter) ? simulator : new UnconfiguredProcessorPort();
    }
}
