package com.altbank.cardsapi.application.port;

import com.altbank.cardsapi.domain.model.DeliveryStatus;

public interface CarrierPort {
    CarrierShipment createShipment(String deliveryAddress);
    record CarrierShipment(String trackingId, DeliveryStatus initialStatus) {
    }
}
