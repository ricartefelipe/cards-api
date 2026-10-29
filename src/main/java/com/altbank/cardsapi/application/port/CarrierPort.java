package com.altbank.cardsapi.application.port;

import com.altbank.cardsapi.domain.model.DeliveryStatus;
import java.util.UUID;

/**
 * Transportadora. O identificador do pedido acompanha a chamada para que um parceiro com
 * contrato de idempotencia possa reconhecer um reenvio; sem esse contrato, nada e reenviado.
 * Uma excecao significa resultado desconhecido, nao recusa.
 */
public interface CarrierPort {

    ShipmentOutcome createShipment(ShipmentOrder order);

    record ShipmentOrder(UUID requestId, String deliveryAddress) {
    }

    sealed interface ShipmentOutcome permits Accepted, Rejected {
    }

    record Accepted(String trackingId, DeliveryStatus initialStatus) implements ShipmentOutcome {
    }

    record Rejected(String reason) implements ShipmentOutcome {
    }
}
