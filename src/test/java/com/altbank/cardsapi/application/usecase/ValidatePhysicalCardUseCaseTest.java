package com.altbank.cardsapi.application.usecase;

import com.altbank.cardsapi.application.dto.ValidatePhysicalCardResponse;
import com.altbank.cardsapi.application.exception.ConflictException;
import com.altbank.cardsapi.application.exception.ErrorCode;
import com.altbank.cardsapi.application.exception.ForbiddenException;
import com.altbank.cardsapi.application.exception.NotFoundException;
import com.altbank.cardsapi.application.port.PhysicalCardRepository;
import com.altbank.cardsapi.application.security.OwnershipPolicy;
import com.altbank.cardsapi.domain.model.Account;
import com.altbank.cardsapi.domain.model.PhysicalCard;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.altbank.cardsapi.CardsTestFixtures.CLOCK;
import static com.altbank.cardsapi.CardsTestFixtures.accountOwnedBy;
import static com.altbank.cardsapi.CardsTestFixtures.activeAccount;
import static com.altbank.cardsapi.CardsTestFixtures.activePhysicalDeliveredNotValidated;
import static com.altbank.cardsapi.CardsTestFixtures.activePhysicalUndelivered;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ValidatePhysicalCardUseCaseTest {

    @Mock
    PhysicalCardRepository physicalCardRepository;

    @Mock
    OwnershipPolicy ownershipPolicy;

    private ValidatePhysicalCardUseCase useCase() {
        return new ValidatePhysicalCardUseCase(physicalCardRepository, ownershipPolicy, CLOCK);
    }

    @Test
    void validate_whenMissing_shouldFail() {
        UUID id = UUID.randomUUID();
        when(physicalCardRepository.findById(id)).thenReturn(Optional.empty());

        NotFoundException ex = Assertions.assertThrows(NotFoundException.class, () -> useCase().validate(id));
        Assertions.assertEquals(ErrorCode.CARD_NOT_FOUND, ex.errorCode());
    }

    @Test
    void validate_whenCallerIsNotOwner_shouldFailAsNotFound() {
        Account account = accountOwnedBy("owner-b");
        PhysicalCard card = activePhysicalDeliveredNotValidated(account);
        when(physicalCardRepository.findById(card.id())).thenReturn(Optional.of(card));
        when(ownershipPolicy.canAccess(account)).thenReturn(false);

        NotFoundException ex = Assertions.assertThrows(NotFoundException.class, () -> useCase().validate(card.id()));
        Assertions.assertEquals(ErrorCode.CARD_NOT_FOUND, ex.errorCode());
        Assertions.assertNull(card.validatedAt());
    }

    @Test
    void validate_whenNotDelivered_shouldFail() {
        Account account = activeAccount();
        PhysicalCard card = activePhysicalUndelivered(account);

        when(physicalCardRepository.findById(card.id())).thenReturn(Optional.of(card));
        when(ownershipPolicy.canAccess(any())).thenReturn(true);

        ConflictException ex = Assertions.assertThrows(ConflictException.class, () -> useCase().validate(card.id()));
        Assertions.assertEquals(ErrorCode.PHYSICAL_CARD_NOT_DELIVERED, ex.errorCode());
    }

    @Test
    void validate_whenAccountCancelled_shouldFail() {
        Account account = activeAccount();
        PhysicalCard card = activePhysicalUndelivered(account);
        account.cancel(CLOCK);

        when(physicalCardRepository.findById(card.id())).thenReturn(Optional.of(card));
        when(ownershipPolicy.canAccess(any())).thenReturn(true);

        ForbiddenException ex = Assertions.assertThrows(ForbiddenException.class, () -> useCase().validate(card.id()));
        Assertions.assertEquals(ErrorCode.ACCOUNT_CANCELLED, ex.errorCode());
    }

    @Test
    void validate_whenInactive_shouldFail() {
        Account account = activeAccount();
        PhysicalCard card = activePhysicalDeliveredNotValidated(account);
        card.deactivate(CLOCK);

        when(physicalCardRepository.findById(card.id())).thenReturn(Optional.of(card));
        when(ownershipPolicy.canAccess(any())).thenReturn(true);

        ForbiddenException ex = Assertions.assertThrows(ForbiddenException.class, () -> useCase().validate(card.id()));
        Assertions.assertEquals(ErrorCode.CARD_INACTIVE, ex.errorCode());
    }

    @Test
    void validate_whenDelivered_shouldSucceedAndBeIdempotentThroughDomain() {
        Account account = activeAccount();
        PhysicalCard card = activePhysicalDeliveredNotValidated(account);

        when(physicalCardRepository.findById(card.id())).thenReturn(Optional.of(card));
        when(ownershipPolicy.canAccess(any())).thenReturn(true);

        ValidatePhysicalCardResponse r1 = useCase().validate(card.id());
        Assertions.assertNotNull(r1.validatedAt());

        ValidatePhysicalCardResponse r2 = useCase().validate(card.id());
        Assertions.assertEquals(r1.validatedAt(), r2.validatedAt());
    }
}
