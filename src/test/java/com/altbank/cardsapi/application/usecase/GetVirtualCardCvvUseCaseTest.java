package com.altbank.cardsapi.application.usecase;

import com.altbank.cardsapi.application.dto.CvvResponse;
import com.altbank.cardsapi.application.exception.ErrorCode;
import com.altbank.cardsapi.application.exception.ForbiddenException;
import com.altbank.cardsapi.application.exception.NotFoundException;
import com.altbank.cardsapi.application.port.ProcessorPort;
import com.altbank.cardsapi.application.port.VirtualCardRepository;
import com.altbank.cardsapi.application.security.OwnershipPolicy;
import com.altbank.cardsapi.domain.model.Account;
import com.altbank.cardsapi.domain.model.VirtualCard;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.altbank.cardsapi.CardsTestFixtures.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetVirtualCardCvvUseCaseTest {

    @Mock
    VirtualCardRepository virtualCardRepository;

    @Mock
    ProcessorPort processorPort;

    @Mock
    OwnershipPolicy ownershipPolicy;

    private GetVirtualCardCvvUseCase useCase() {
        return new GetVirtualCardCvvUseCase(virtualCardRepository, processorPort, ownershipPolicy);
    }

    @Test
    void get_whenMissing_shouldFail() {
        UUID id = UUID.randomUUID();
        when(virtualCardRepository.findById(id)).thenReturn(Optional.empty());

        NotFoundException ex = Assertions.assertThrows(NotFoundException.class, () -> useCase().get(id));
        Assertions.assertEquals(ErrorCode.VIRTUAL_CARD_NOT_FOUND, ex.errorCode());
        verifyNoInteractions(processorPort);
    }

    @Test
    void get_whenCallerIsNotOwner_shouldFailAsNotFound() {
        Account account = accountOwnedBy("owner-b");
        VirtualCard card = activeVirtual(account, "pc_b");
        when(virtualCardRepository.findById(card.id())).thenReturn(Optional.of(card));
        when(ownershipPolicy.canAccess(account)).thenReturn(false);

        NotFoundException ex = Assertions.assertThrows(NotFoundException.class, () -> useCase().get(card.id()));
        Assertions.assertEquals(ErrorCode.VIRTUAL_CARD_NOT_FOUND, ex.errorCode());
        verifyNoInteractions(processorPort);
    }

    @Test
    void get_whenAccountCancelled_shouldFail() {
        Account account = activeAccount();
        account.cancel(CLOCK);
        VirtualCard card = activeVirtual(account, "pc_1");

        when(virtualCardRepository.findById(card.id())).thenReturn(Optional.of(card));
        when(ownershipPolicy.canAccess(any())).thenReturn(true);

        ForbiddenException ex = Assertions.assertThrows(ForbiddenException.class, () -> useCase().get(card.id()));
        Assertions.assertEquals(ErrorCode.ACCOUNT_CANCELLED, ex.errorCode());
        verifyNoInteractions(processorPort);
    }

    @Test
    void get_whenCardInactive_shouldFail() {
        Account account = activeAccount();
        VirtualCard card = activeVirtual(account, "pc_2");
        card.deactivate(CLOCK);

        when(virtualCardRepository.findById(card.id())).thenReturn(Optional.of(card));
        when(ownershipPolicy.canAccess(any())).thenReturn(true);

        ForbiddenException ex = Assertions.assertThrows(ForbiddenException.class, () -> useCase().get(card.id()));
        Assertions.assertEquals(ErrorCode.CARD_INACTIVE, ex.errorCode());
        verifyNoInteractions(processorPort);
    }

    @Test
    void get_whenOwnerAndActive_shouldDelegateToProcessor() {
        Account account = accountOwnedBy("owner-a");
        VirtualCard card = activeVirtual(account, "pc_ok");
        when(virtualCardRepository.findById(card.id())).thenReturn(Optional.of(card));
        when(ownershipPolicy.canAccess(account)).thenReturn(true);
        when(processorPort.getCurrentCvv("pc_ok"))
                .thenReturn(new ProcessorPort.CvvData(321, LocalDateTime.of(2026, 1, 31, 13, 0, 0)));

        CvvResponse response = useCase().get(card.id());
        Assertions.assertEquals(321, response.cvv());
    }
}
