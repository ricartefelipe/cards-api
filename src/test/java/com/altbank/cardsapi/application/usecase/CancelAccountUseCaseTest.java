package com.altbank.cardsapi.application.usecase;

import com.altbank.cardsapi.application.dto.CancelAccountResponse;
import com.altbank.cardsapi.application.exception.ErrorCode;
import com.altbank.cardsapi.application.exception.NotFoundException;
import com.altbank.cardsapi.application.port.AccountRepository;
import com.altbank.cardsapi.application.port.PhysicalCardRepository;
import com.altbank.cardsapi.application.port.ProcessorPort;
import com.altbank.cardsapi.application.port.VirtualCardRepository;
import com.altbank.cardsapi.domain.model.Account;
import com.altbank.cardsapi.domain.model.PhysicalCard;
import com.altbank.cardsapi.domain.model.VirtualCard;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.altbank.cardsapi.CardsTestFixtures.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CancelAccountUseCaseTest {

    @Mock
    AccountRepository accountRepository;
    @Mock
    PhysicalCardRepository physicalCardRepository;
    @Mock
    VirtualCardRepository virtualCardRepository;
    @Mock
    ProcessorPort processorPort;

    @Test
    void cancel_whenAccountMissing_shouldFail() {
        UUID id = UUID.randomUUID();
        when(accountRepository.findById(id)).thenReturn(Optional.empty());

        CancelAccountUseCase useCase = new CancelAccountUseCase(
                accountRepository, physicalCardRepository, virtualCardRepository, processorPort, CLOCK);

        NotFoundException ex = Assertions.assertThrows(NotFoundException.class, () -> useCase.cancel(id));
        Assertions.assertEquals(ErrorCode.ACCOUNT_NOT_FOUND, ex.errorCode());
        verifyNoInteractions(processorPort);
    }

    @Test
    void cancel_shouldDeactivateCardsAndTellProcessorWhenVirtualPresent() {
        Account account = activeAccount();
        UUID accountId = account.id();
        PhysicalCard physical = activePhysicalUndelivered(account);
        VirtualCard virt = activeVirtual(account, "pc_xyz");

        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));
        when(physicalCardRepository.findActiveByAccountId(accountId)).thenReturn(Optional.of(physical));
        when(virtualCardRepository.findActiveByAccountId(accountId)).thenReturn(Optional.of(virt));

        CancelAccountUseCase useCase = new CancelAccountUseCase(
                accountRepository, physicalCardRepository, virtualCardRepository, processorPort, CLOCK);

        CancelAccountResponse response = useCase.cancel(accountId);

        Assertions.assertEquals(accountId, response.accountId());
        Assertions.assertNotNull(response.cancelledAt());
        Assertions.assertFalse(account.isActive());
        Assertions.assertFalse(physical.isActive());
        Assertions.assertFalse(virt.isActive());
        verify(processorPort).deactivateCard("pc_xyz");
    }

    @Test
    void cancel_whenNoCards_shouldStillCancelAccount() {
        Account account = activeAccount();
        UUID accountId = account.id();

        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));
        when(physicalCardRepository.findActiveByAccountId(accountId)).thenReturn(Optional.empty());
        when(virtualCardRepository.findActiveByAccountId(accountId)).thenReturn(Optional.empty());

        CancelAccountUseCase useCase = new CancelAccountUseCase(
                accountRepository, physicalCardRepository, virtualCardRepository, processorPort, CLOCK);

        CancelAccountResponse response = useCase.cancel(accountId);

        Assertions.assertEquals(accountId, response.accountId());
        verifyNoInteractions(processorPort);
    }
}
