package com.altbank.cardsapi.infrastructure.external;

import com.altbank.cardsapi.application.port.CarrierPort;
import io.quarkus.arc.profile.IfBuildProfile;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Adaptador fail-closed de transportadora para producao sem parceiro contratado.
 * Satisfaz a injecao de {@link CarrierPort}; qualquer operacao real falha explicitamente.
 * Barreira secundaria ao ProductionReadinessGuard.
 */
@ApplicationScoped
@IfBuildProfile("prod")
public class UnconfiguredCarrierPort implements CarrierPort {

    @Override
    public CarrierShipment createShipment(String deliveryAddress) {
        throw new IllegalStateException(
                "Transportadora nao configurada: nenhum adaptador real contratado esta ativo em producao.");
    }
}
