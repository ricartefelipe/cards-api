package com.altbank.cardsapi.infrastructure.config;

import io.quarkus.runtime.StartupEvent;
import io.quarkus.runtime.configuration.ConfigUtils;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import java.util.List;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Bloqueia a inicializacao em producao quando nao ha adaptador real de processadora/
 * transportadora contratado (modalidade sandbox/kit).
 *
 * <p>A decisao usa o perfil de <em>execucao</em>, nao o de build: a mesma imagem final
 * (construida no perfil prod) pode subir no perfil {@code demo} com simuladores, e continua
 * recusando iniciar quando o perfil de execucao e {@code prod}.
 */
@ApplicationScoped
public class ProductionReadinessGuard {

    static final String PRODUCTION_PROFILE = "prod";

    private final String processorAdapter;
    private final String carrierAdapter;

    public ProductionReadinessGuard(
            @ConfigProperty(name = "cards.processor.adapter") String processorAdapter,
            @ConfigProperty(name = "cards.carrier.adapter") String carrierAdapter) {
        this.processorAdapter = processorAdapter;
        this.carrierAdapter = carrierAdapter;
    }

    void onStart(@Observes StartupEvent event) {
        if (!ConfigUtils.getProfiles().contains(PRODUCTION_PROFILE)) {
            return;
        }
        List<String> problems = ProductionReadiness.problems(processorAdapter, carrierAdapter);
        if (!problems.isEmpty()) {
            throw new IllegalStateException(
                    "Inicializacao de producao recusada (fail-closed). Pendencias: " + String.join("; ", problems));
        }
    }
}
