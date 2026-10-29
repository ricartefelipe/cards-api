package com.altbank.cardsapi.application.usecase;

import static com.altbank.cardsapi.CardsTestFixtures.CLOCK;
import static com.altbank.cardsapi.CardsTestFixtures.activeAccount;
import static com.altbank.cardsapi.CardsTestFixtures.validCreateAccountRequest;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.altbank.cardsapi.application.audit.AuditService;
import com.altbank.cardsapi.application.dto.CreateAccountRequest;
import com.altbank.cardsapi.application.dto.CreateAccountResponse;
import com.altbank.cardsapi.application.exception.ConflictException;
import com.altbank.cardsapi.application.exception.ErrorCode;
import com.altbank.cardsapi.application.port.AccountRepository;
import com.altbank.cardsapi.application.port.CustomerRepository;
import com.altbank.cardsapi.application.port.PhysicalCardRepository;
import com.altbank.cardsapi.application.port.TransactionRunner;
import com.altbank.cardsapi.application.shipping.ShipmentOrchestrator;
import com.altbank.cardsapi.domain.model.Customer;
import com.altbank.cardsapi.domain.model.DeliveryStatus;
import com.altbank.cardsapi.domain.model.PhysicalCard;
import com.altbank.cardsapi.domain.model.ShipmentStatus;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CreateAccountUseCaseTest {

    /** Sem transacao de verdade no teste unitario: o trecho roda em linha. */
    static final TransactionRunner INLINE = new TransactionRunner() {
        @Override
        public <T> T inTransaction(Supplier<T> work) {
            return work.get();
        }
    };

    @Mock
    CustomerRepository customerRepository;
    @Mock
    AccountRepository accountRepository;
    @Mock
    PhysicalCardRepository physicalCardRepository;
    @Mock
    ShipmentOrchestrator shipments;
    @Mock
    AuditService auditService;

    private CreateAccountUseCase useCase() {
        return new CreateAccountUseCase(
                customerRepository, accountRepository, physicalCardRepository, shipments, INLINE, auditService, CLOCK);
    }

    @Test
    void create_whenDocumentExists_shouldFail() {
        CreateAccountRequest req = validCreateAccountRequest();
        Customer existing = activeAccount().customer();
        when(customerRepository.findByDocument("99887766554")).thenReturn(Optional.of(existing));

        ConflictException ex = Assertions.assertThrows(ConflictException.class, () -> useCase().create(req));

        Assertions.assertEquals(ErrorCode.CUSTOMER_ALREADY_EXISTS, ex.errorCode());
        verify(accountRepository, never()).persist(any());
        verify(physicalCardRepository, never()).persist(any());
        verifyNoInteractions(shipments);
    }

    @Test
    void create_shouldPersistCardWithoutTrackingThenDispatchTheShipment() {
        CreateAccountRequest req = validCreateAccountRequest();
        when(customerRepository.findByDocument("99887766554")).thenReturn(Optional.empty());
        UUID requestId = UUID.randomUUID();
        when(shipments.requestShipment(any(PhysicalCard.class), any(), eq(CreateAccountUseCase.ACTOR))).thenReturn(requestId);
        when(shipments.dispatch(requestId, CreateAccountUseCase.ACTOR))
                .thenReturn(new ShipmentOrchestrator.Dispatch(ShipmentStatus.CONFIRMED, Optional.of("trk_new")));

        CreateAccountResponse response = useCase().create(req);

        Assertions.assertNotNull(response.accountId());
        Assertions.assertEquals("trk_new", response.trackingId());
        Assertions.assertEquals("CONFIRMED", response.shipmentStatus());

        ArgumentCaptor<PhysicalCard> card = ArgumentCaptor.forClass(PhysicalCard.class);
        verify(physicalCardRepository).persist(card.capture());
        Assertions.assertTrue(card.getValue().awaitsShipment(), "o cartao nasce sem tracking");
        Assertions.assertEquals(DeliveryStatus.PENDING, card.getValue().deliveryStatus());
        ArgumentCaptor<String> address = ArgumentCaptor.forClass(String.class);
        verify(shipments).requestShipment(any(PhysicalCard.class), address.capture(), eq(CreateAccountUseCase.ACTOR));
        Assertions.assertTrue(address.getValue().contains("Av B"));
    }

    @Test
    void create_whenCarrierOutcomeIsUnknown_stillAnswersWithTheCardAndNoTracking() {
        CreateAccountRequest req = validCreateAccountRequest();
        when(customerRepository.findByDocument("99887766554")).thenReturn(Optional.empty());
        UUID requestId = UUID.randomUUID();
        when(shipments.requestShipment(any(PhysicalCard.class), any(), any())).thenReturn(requestId);
        when(shipments.dispatch(requestId, CreateAccountUseCase.ACTOR))
                .thenReturn(new ShipmentOrchestrator.Dispatch(ShipmentStatus.UNCERTAIN, Optional.empty()));

        CreateAccountResponse response = useCase().create(req);

        Assertions.assertNull(response.trackingId());
        Assertions.assertEquals("UNCERTAIN", response.shipmentStatus());
        verify(physicalCardRepository).persist(any(PhysicalCard.class));
    }
}
