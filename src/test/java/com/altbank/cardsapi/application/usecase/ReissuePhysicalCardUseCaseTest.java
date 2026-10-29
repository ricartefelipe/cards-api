package com.altbank.cardsapi.application.usecase;

import com.altbank.cardsapi.application.dto.ReissuePhysicalCardRequest;
import com.altbank.cardsapi.application.dto.ReissuePhysicalCardResponse;
import com.altbank.cardsapi.application.exception.ErrorCode;
import com.altbank.cardsapi.application.exception.ForbiddenException;
import com.altbank.cardsapi.application.exception.NotFoundException;
import com.altbank.cardsapi.application.port.TransactionRunner;
import com.altbank.cardsapi.application.shipping.ShipmentOrchestrator;
import com.altbank.cardsapi.domain.model.ShipmentStatus;
import java.util.function.Supplier;
import com.altbank.cardsapi.application.port.PhysicalCardRepository;
import com.altbank.cardsapi.application.security.OwnershipPolicy;
import com.altbank.cardsapi.domain.model.Account;
import com.altbank.cardsapi.domain.model.PhysicalCard;
import com.altbank.cardsapi.domain.model.ReissueReason;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.altbank.cardsapi.CardsTestFixtures.CLOCK;
import static com.altbank.cardsapi.CardsTestFixtures.accountOwnedBy;
import static com.altbank.cardsapi.CardsTestFixtures.activeAccount;
import static com.altbank.cardsapi.CardsTestFixtures.activePhysicalUndelivered;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReissuePhysicalCardUseCaseTest {

    @Mock
    PhysicalCardRepository physicalCardRepository;

    @Mock
    ShipmentOrchestrator shipments;

    static final TransactionRunner INLINE = new TransactionRunner() {
        @Override
        public <T> T inTransaction(Supplier<T> work) {
            return work.get();
        }
    };

    @Mock
    OwnershipPolicy ownershipPolicy;

    @Test
    void reissue_whenMissing_shouldFail() {
        UUID id = UUID.randomUUID();
        when(physicalCardRepository.findById(id)).thenReturn(Optional.empty());

        ReissuePhysicalCardUseCase useCase = new ReissuePhysicalCardUseCase(physicalCardRepository, shipments, INLINE, ownershipPolicy, CLOCK);

        NotFoundException ex = Assertions.assertThrows(
                NotFoundException.class,
                () -> useCase.reissue(id, new ReissuePhysicalCardRequest(ReissueReason.LOSS)));

        Assertions.assertEquals(ErrorCode.CARD_NOT_FOUND, ex.errorCode());
        verifyNoInteractions(shipments);
    }

    @Test
    void reissue_whenAccountCancelled_shouldFail() {
        Account account = activeAccount();
        account.cancel(CLOCK);
        PhysicalCard card = activePhysicalUndelivered(account);

        when(physicalCardRepository.findById(card.id())).thenReturn(Optional.of(card));
        when(ownershipPolicy.canAccess(any())).thenReturn(true);

        ReissuePhysicalCardUseCase useCase = new ReissuePhysicalCardUseCase(physicalCardRepository, shipments, INLINE, ownershipPolicy, CLOCK);

        ForbiddenException ex = Assertions.assertThrows(
                ForbiddenException.class,
                () -> useCase.reissue(card.id(), new ReissuePhysicalCardRequest(ReissueReason.DAMAGE)));

        Assertions.assertEquals(ErrorCode.ACCOUNT_CANCELLED, ex.errorCode());
        verifyNoInteractions(shipments);
    }

    @Test
    void reissue_whenInactiveCard_shouldFail() {
        Account account = activeAccount();
        PhysicalCard card = activePhysicalUndelivered(account);
        card.deactivate(CLOCK);

        when(physicalCardRepository.findById(card.id())).thenReturn(Optional.of(card));
        when(ownershipPolicy.canAccess(any())).thenReturn(true);

        ReissuePhysicalCardUseCase useCase = new ReissuePhysicalCardUseCase(physicalCardRepository, shipments, INLINE, ownershipPolicy, CLOCK);

        ForbiddenException ex = Assertions.assertThrows(
                ForbiddenException.class,
                () -> useCase.reissue(card.id(), new ReissuePhysicalCardRequest(ReissueReason.THEFT)));

        Assertions.assertEquals(ErrorCode.CARD_INACTIVE, ex.errorCode());
        verify(shipments, never()).requestShipment(any(), any(), any());
    }

    @Test
    void reissue_shouldDeactivateOldAndPersistNew() {
        Account account = activeAccount();
        PhysicalCard old = activePhysicalUndelivered(account);

        when(physicalCardRepository.findById(old.id())).thenReturn(Optional.of(old));
        when(ownershipPolicy.canAccess(any())).thenReturn(true);
        java.util.UUID requestId = java.util.UUID.randomUUID();
        when(shipments.requestShipment(any(PhysicalCard.class), any(), any())).thenReturn(requestId);
        when(shipments.dispatch(requestId, ReissuePhysicalCardUseCase.ACTOR))
                .thenReturn(new ShipmentOrchestrator.Dispatch(ShipmentStatus.CONFIRMED, java.util.Optional.of("trk_re")));

        ReissuePhysicalCardUseCase useCase = new ReissuePhysicalCardUseCase(physicalCardRepository, shipments, INLINE, ownershipPolicy, CLOCK);

        ReissuePhysicalCardResponse response =
                useCase.reissue(old.id(), new ReissuePhysicalCardRequest(ReissueReason.LOSS));

        Assertions.assertFalse(old.isActive());
        Assertions.assertEquals("trk_re", response.trackingId());

        ArgumentCaptor<PhysicalCard> captor = ArgumentCaptor.forClass(PhysicalCard.class);
        verify(physicalCardRepository).persist(captor.capture());
        PhysicalCard created = captor.getValue();
        Assertions.assertEquals(ReissueReason.LOSS, created.reissueReason());
        Assertions.assertEquals(old.id().toString(), created.previousPhysicalCardId());
        Assertions.assertTrue(created.isActive());
        Assertions.assertTrue(created.awaitsShipment(), "o tracking so chega pela transportadora");
        Assertions.assertEquals("CONFIRMED", response.shipmentStatus());
    }

    @Test
    void reissue_whenCallerIsNotOwner_shouldFailAsNotFoundWithoutSideEffects() {
        Account account = accountOwnedBy("owner-b");
        PhysicalCard old = activePhysicalUndelivered(account);
        when(physicalCardRepository.findById(old.id())).thenReturn(Optional.of(old));
        when(ownershipPolicy.canAccess(account)).thenReturn(false);

        ReissuePhysicalCardUseCase useCase = new ReissuePhysicalCardUseCase(physicalCardRepository, shipments, INLINE, ownershipPolicy, CLOCK);

        NotFoundException ex = Assertions.assertThrows(
                NotFoundException.class,
                () -> useCase.reissue(old.id(), new ReissuePhysicalCardRequest(ReissueReason.LOSS)));

        Assertions.assertEquals(ErrorCode.CARD_NOT_FOUND, ex.errorCode());
        Assertions.assertTrue(old.isActive());
        verifyNoInteractions(shipments);
        verify(physicalCardRepository, never()).persist(any(PhysicalCard.class));
    }
}
