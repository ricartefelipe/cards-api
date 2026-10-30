package com.altbank.cardsapi.infrastructure.external;

import com.altbank.cardsapi.application.port.CarrierPort;

/**
 * Adaptador fail-closed de transportadora para producao sem parceiro contratado.
 * Satisfaz a injecao de {@link CarrierPort}; qualquer operacao real falha explicitamente.
 * Barreira secundaria ao ProductionReadinessGuard; instanciado por {@link ExternalAdapterProducers}.
 */
public class UnconfiguredCarrierPort implements CarrierPort {

    @Override
    public ShipmentOutcome createShipment(ShipmentOrder order) {
        throw new IllegalStateException(
                "Transportadora nao configurada: nenhum adaptador real contratado esta ativo em producao.");
    }
}
