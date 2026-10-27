package com.altbank.cardsapi.infrastructure.config;

import java.util.ArrayList;
import java.util.List;

/**
 * Regras puras de prontidao para producao (fail-closed). Sem dependencia de framework
 * para permitir teste direto. Um resultado nao vazio deve impedir a inicializacao.
 */
public final class ProductionReadiness {

    public static final String SIMULATOR = "simulator";

    private ProductionReadiness() {
    }

    /**
     * @return lista de pendencias que bloqueiam a producao; vazia quando pronto.
     */
    public static List<String> problems(String processorAdapter, String carrierAdapter) {
        List<String> problems = new ArrayList<>();
        if (isSimulator(processorAdapter)) {
            problems.add("processadora: adaptador real nao configurado "
                    + "(pendente: PROCESSOR_ADAPTER=<provedor homologado> e implementacao do adaptador)");
        }
        if (isSimulator(carrierAdapter)) {
            problems.add("transportadora: adaptador real nao configurado "
                    + "(pendente: CARRIER_ADAPTER=<provedor homologado> e implementacao do adaptador)");
        }
        return List.copyOf(problems);
    }

    private static boolean isSimulator(String adapter) {
        return adapter == null || adapter.isBlank() || SIMULATOR.equalsIgnoreCase(adapter.strip());
    }
}
