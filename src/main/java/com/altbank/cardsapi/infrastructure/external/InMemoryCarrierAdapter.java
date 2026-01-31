package com.altbank.cardsapi.infrastructure.external;

import jakarta.inject.Inject;
import com.altbank.cardsapi.application.port.CarrierPort;
import com.altbank.cardsapi.domain.model.DeliveryStatus;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.UUID;

@ApplicationScoped
public class InMemoryCarrierAdapter implements CarrierPort {

    @Override
    public CarrierShipment createShipment(String deliveryAddress) {
        String trackingId = "trk_" + UUID.randomUUID();
        return new CarrierShipment(trackingId, DeliveryStatus.SHIPPED);
    }
}
