package com.altbank.cardsapi.application.usecase;

import com.altbank.cardsapi.application.dto.CarrierDeliveryWebhookRequest;
import com.altbank.cardsapi.application.dto.WebhookAckResponse;
import com.altbank.cardsapi.application.exception.ErrorCode;
import com.altbank.cardsapi.application.exception.NotFoundException;
import com.altbank.cardsapi.application.port.PhysicalCardRepository;
import com.altbank.cardsapi.domain.model.Account;
import com.altbank.cardsapi.domain.model.DeliveryStatus;
import com.altbank.cardsapi.domain.model.PhysicalCard;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.altbank.cardsapi.CardsTestFixtures.CLOCK;
import static com.altbank.cardsapi.CardsTestFixtures.activeAccount;
import static com.altbank.cardsapi.CardsTestFixtures.activePhysicalUndelivered;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HandleCarrierDeliveryWebhookUseCaseTest {

    @Mock
    PhysicalCardRepository physicalCardRepository;

    @Test
    void handle_whenUnknownTracking_shouldFail() {
        when(physicalCardRepository.findByTrackingId("missing")).thenReturn(Optional.empty());

        HandleCarrierDeliveryWebhookUseCase useCase = new HandleCarrierDeliveryWebhookUseCase(physicalCardRepository, CLOCK);

        CarrierDeliveryWebhookRequest req = new CarrierDeliveryWebhookRequest(
                "missing", "DELIVERED", LocalDateTime.of(2026, 2, 1, 10, 0), null, null);

        NotFoundException ex = Assertions.assertThrows(NotFoundException.class, () -> useCase.handle(req));
        Assertions.assertEquals(ErrorCode.CARD_NOT_FOUND, ex.errorCode());
    }

    @Test
    void handle_shouldUpdateCarrierState() {
        Account account = activeAccount();
        PhysicalCard physical = activePhysicalUndelivered(account);

        when(physicalCardRepository.findByTrackingId(physical.trackingId())).thenReturn(Optional.of(physical));

        HandleCarrierDeliveryWebhookUseCase useCase = new HandleCarrierDeliveryWebhookUseCase(physicalCardRepository, CLOCK);

        CarrierDeliveryWebhookRequest req = new CarrierDeliveryWebhookRequest(
                physical.trackingId(),
                "DELIVERED",
                LocalDateTime.of(2026, 2, 1, 11, 0),
                null,
                "addr");

        WebhookAckResponse ack = useCase.handle(req);

        Assertions.assertEquals("OK", ack.status());
        Assertions.assertEquals(DeliveryStatus.DELIVERED, physical.deliveryStatus());
        Assertions.assertNotNull(physical.deliveredAt());
    }
}
