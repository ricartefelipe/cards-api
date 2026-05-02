package com.altbank.cardsapi.application.usecase;

import com.altbank.cardsapi.application.dto.ValidatePhysicalCardResponse;
import com.altbank.cardsapi.application.exception.ConflictException;
import com.altbank.cardsapi.application.exception.ErrorCode;
import com.altbank.cardsapi.application.exception.ForbiddenException;
import com.altbank.cardsapi.application.exception.NotFoundException;
import com.altbank.cardsapi.application.port.PhysicalCardRepository;
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
import static com.altbank.cardsapi.CardsTestFixtures.activeAccount;
import static com.altbank.cardsapi.CardsTestFixtures.activePhysicalDeliveredNotValidated;
import static com.altbank.cardsapi.CardsTestFixtures.activePhysicalUndelivered;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ValidatePhysicalCardUseCaseTest {

    @Mock
    PhysicalCardRepository physicalCardRepository;

    @Test
    void validate_whenMissing_shouldFail() {
        UUID id = UUID.randomUUID();
        when(physicalCardRepository.findById(id)).thenReturn(Optional.empty());

        ValidatePhysicalCardUseCase useCase = new ValidatePhysicalCardUseCase(physicalCardRepository, CLOCK);

        NotFoundException ex = Assertions.assertThrows(NotFoundException.class, () -> useCase.validate(id));
        Assertions.assertEquals(ErrorCode.CARD_NOT_FOUND, ex.errorCode());
    }

    @Test
    void validate_whenNotDelivered_shouldFail() {
        Account account = activeAccount();
        PhysicalCard card = activePhysicalUndelivered(account);

        when(physicalCardRepository.findById(card.id())).thenReturn(Optional.of(card));

        ValidatePhysicalCardUseCase useCase = new ValidatePhysicalCardUseCase(physicalCardRepository, CLOCK);

        ConflictException ex = Assertions.assertThrows(ConflictException.class, () -> useCase.validate(card.id()));
        Assertions.assertEquals(ErrorCode.PHYSICAL_CARD_NOT_DELIVERED, ex.errorCode());
    }

    @Test
    void validate_whenAccountCancelled_shouldFail() {
        Account account = activeAccount();
        PhysicalCard card = activePhysicalUndelivered(account);
        account.cancel(CLOCK);

        when(physicalCardRepository.findById(card.id())).thenReturn(Optional.of(card));

        ValidatePhysicalCardUseCase useCase = new ValidatePhysicalCardUseCase(physicalCardRepository, CLOCK);

        ForbiddenException ex = Assertions.assertThrows(ForbiddenException.class, () -> useCase.validate(card.id()));
        Assertions.assertEquals(ErrorCode.ACCOUNT_CANCELLED, ex.errorCode());
    }

    @Test
    void validate_whenInactive_shouldFail() {
        Account account = activeAccount();
        PhysicalCard card = activePhysicalDeliveredNotValidated(account);
        card.deactivate(CLOCK);

        when(physicalCardRepository.findById(card.id())).thenReturn(Optional.of(card));

        ValidatePhysicalCardUseCase useCase = new ValidatePhysicalCardUseCase(physicalCardRepository, CLOCK);

        ForbiddenException ex = Assertions.assertThrows(ForbiddenException.class, () -> useCase.validate(card.id()));
        Assertions.assertEquals(ErrorCode.CARD_INACTIVE, ex.errorCode());
    }

    @Test
    void validate_whenDelivered_shouldSucceedAndBeIdempotentThroughDomain() {
        Account account = activeAccount();
        PhysicalCard card = activePhysicalDeliveredNotValidated(account);

        when(physicalCardRepository.findById(card.id())).thenReturn(Optional.of(card));

        ValidatePhysicalCardUseCase useCase = new ValidatePhysicalCardUseCase(physicalCardRepository, CLOCK);

        ValidatePhysicalCardResponse r1 = useCase.validate(card.id());
        Assertions.assertNotNull(r1.validatedAt());

        ValidatePhysicalCardResponse r2 = useCase.validate(card.id());
        Assertions.assertEquals(r1.validatedAt(), r2.validatedAt());
    }
}
