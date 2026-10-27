package com.altbank.cardsapi.application.usecase;

import jakarta.inject.Inject;
import com.altbank.cardsapi.application.WebhookEventFingerprint;
import com.altbank.cardsapi.application.dto.CarrierDeliveryWebhookRequest;
import com.altbank.cardsapi.application.dto.WebhookAckResponse;
import com.altbank.cardsapi.application.exception.ErrorCode;
import com.altbank.cardsapi.application.exception.NotFoundException;
import com.altbank.cardsapi.application.port.PhysicalCardRepository;
import com.altbank.cardsapi.application.port.WebhookEventRepository;
import com.altbank.cardsapi.domain.model.DeliveryStatus;
import com.altbank.cardsapi.domain.model.PhysicalCard;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Objects;

@ApplicationScoped
public class HandleCarrierDeliveryWebhookUseCase {

    private static final String EVENT_TYPE = "carrier.delivery";

    private final PhysicalCardRepository physicalCardRepository;
    private final WebhookEventRepository webhookEventRepository;
    private final Clock clock;

    @Inject
    public HandleCarrierDeliveryWebhookUseCase(PhysicalCardRepository physicalCardRepository,
                                               WebhookEventRepository webhookEventRepository,
                                               Clock clock) {
        this.physicalCardRepository = Objects.requireNonNull(physicalCardRepository, "physicalCardRepository");
        this.webhookEventRepository = Objects.requireNonNull(webhookEventRepository, "webhookEventRepository");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    @Transactional
    public WebhookAckResponse handle(CarrierDeliveryWebhookRequest request) {
        PhysicalCard physicalCard = physicalCardRepository.findByTrackingId(request.trackingId())
                .orElseThrow(() -> new NotFoundException(ErrorCode.CARD_NOT_FOUND, "Physical card not found for tracking id"));

        String fingerprint = WebhookEventFingerprint.of(EVENT_TYPE,
                request.trackingId(),
                request.deliveryStatus(),
                Objects.toString(request.deliveryDate(), ""));
        if (!webhookEventRepository.recordIfAbsent(fingerprint, EVENT_TYPE)) {
            return new WebhookAckResponse("OK", LocalDateTime.now(clock));
        }

        DeliveryStatus status = DeliveryStatus.fromExternal(request.deliveryStatus());
        physicalCard.applyCarrierDeliveryUpdate(
                status,
                request.deliveryDate(),
                request.deliveryReturnReason(),
                request.deliveryAddress(),
                clock
        );

        return new WebhookAckResponse("OK", LocalDateTime.now(clock));
    }
}
