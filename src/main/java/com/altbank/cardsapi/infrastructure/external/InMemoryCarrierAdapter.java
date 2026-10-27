package com.altbank.cardsapi.infrastructure.external;

import com.altbank.cardsapi.application.port.CarrierPort;
import com.altbank.cardsapi.domain.model.DeliveryStatus;
import io.quarkus.arc.profile.UnlessBuildProfile;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.UUID;

/**
 * Transportadora simulada para dev/test e demonstracao sintetica.
 * Restrito a perfis diferentes de "prod" (ver ProductionReadinessGuard).
 */
@ApplicationScoped
@UnlessBuildProfile("prod")
public class InMemoryCarrierAdapter implements CarrierPort {

    @Override
    public CarrierShipment createShipment(String deliveryAddress) {
        String trackingId = "trk_" + UUID.randomUUID();
        return new CarrierShipment(trackingId, DeliveryStatus.SHIPPED);
    }
}
