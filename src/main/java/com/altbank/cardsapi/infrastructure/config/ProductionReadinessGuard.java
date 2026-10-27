package com.altbank.cardsapi.infrastructure.config;

import io.quarkus.arc.profile.IfBuildProfile;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import java.util.List;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Bloqueia a inicializacao em producao quando nao ha adaptador real de processadora/
 * transportadora contratado (modalidade sandbox/kit). Ativo apenas no perfil "prod".
 * Os segredos obrigatorios ja falham antes por nao terem default em prod.
 */
@ApplicationScoped
@IfBuildProfile("prod")
public class ProductionReadinessGuard {

    private final String processorAdapter;
    private final String carrierAdapter;

    public ProductionReadinessGuard(
            @ConfigProperty(name = "cards.processor.adapter") String processorAdapter,
            @ConfigProperty(name = "cards.carrier.adapter") String carrierAdapter) {
        this.processorAdapter = processorAdapter;
        this.carrierAdapter = carrierAdapter;
    }

    void onStart(@Observes StartupEvent event) {
        List<String> problems = ProductionReadiness.problems(processorAdapter, carrierAdapter);
        if (!problems.isEmpty()) {
            throw new IllegalStateException(
                    "Inicializacao de producao recusada (fail-closed). Pendencias: " + String.join("; ", problems));
        }
    }
}
