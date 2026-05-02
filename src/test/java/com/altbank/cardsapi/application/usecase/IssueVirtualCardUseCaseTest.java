package com.altbank.cardsapi.application.usecase;

import com.altbank.cardsapi.application.exception.ConflictException;
import com.altbank.cardsapi.application.exception.ErrorCode;
import com.altbank.cardsapi.application.exception.ForbiddenException;
import com.altbank.cardsapi.application.exception.NotFoundException;
import com.altbank.cardsapi.application.port.AccountRepository;
import com.altbank.cardsapi.application.port.PhysicalCardRepository;
import com.altbank.cardsapi.application.port.ProcessorPort;
import com.altbank.cardsapi.application.port.VirtualCardRepository;
import com.altbank.cardsapi.domain.model.Account;
import com.altbank.cardsapi.domain.model.PhysicalCard;
import com.altbank.cardsapi.domain.model.VirtualCard;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.altbank.cardsapi.CardsTestFixtures.CLOCK;
import static com.altbank.cardsapi.CardsTestFixtures.activeAccount;
import static com.altbank.cardsapi.CardsTestFixtures.activePhysicalDeliveredNotValidated;
import static com.altbank.cardsapi.CardsTestFixtures.activePhysicalDeliveredValidated;
import static com.altbank.cardsapi.CardsTestFixtures.activePhysicalUndelivered;
import static com.altbank.cardsapi.CardsTestFixtures.activeVirtual;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IssueVirtualCardUseCaseTest {

    @Mock
    AccountRepository accountRepository;

    @Mock
    PhysicalCardRepository physicalCardRepository;

    @Mock
    VirtualCardRepository virtualCardRepository;

    @Mock
    ProcessorPort processorPort;

    @Test
    void issue_whenAccountMissing_shouldFail() {
        UUID id = UUID.randomUUID();
        when(accountRepository.findById(id)).thenReturn(Optional.empty());

        IssueVirtualCardUseCase useCase =
                new IssueVirtualCardUseCase(accountRepository, physicalCardRepository, virtualCardRepository, processorPort, CLOCK);

        NotFoundException ex = Assertions.assertThrows(NotFoundException.class, () -> useCase.issue(id));
        Assertions.assertEquals(ErrorCode.ACCOUNT_NOT_FOUND, ex.errorCode());
        verifyNoInteractions(processorPort);
    }

    @Test
    void issue_whenAccountCancelled_shouldFail() {
        Account account = activeAccount();
        account.cancel(CLOCK);

        when(accountRepository.findById(account.id())).thenReturn(Optional.of(account));

        IssueVirtualCardUseCase useCase =
                new IssueVirtualCardUseCase(accountRepository, physicalCardRepository, virtualCardRepository, processorPort, CLOCK);

        ForbiddenException ex = Assertions.assertThrows(ForbiddenException.class, () -> useCase.issue(account.id()));
        Assertions.assertEquals(ErrorCode.ACCOUNT_CANCELLED, ex.errorCode());
        verify(physicalCardRepository, never()).findActiveByAccountId(account.id());
    }

    @Test
    void issue_whenNoPhysicalCard_shouldFail() {
        Account account = activeAccount();

        when(accountRepository.findById(account.id())).thenReturn(Optional.of(account));
        when(physicalCardRepository.findActiveByAccountId(account.id())).thenReturn(Optional.empty());

        IssueVirtualCardUseCase useCase =
                new IssueVirtualCardUseCase(accountRepository, physicalCardRepository, virtualCardRepository, processorPort, CLOCK);

        NotFoundException ex = Assertions.assertThrows(NotFoundException.class, () -> useCase.issue(account.id()));
        Assertions.assertEquals(ErrorCode.CARD_NOT_FOUND, ex.errorCode());
    }

    @Test
    void issueVirtualCard_beforeDeliveryAndValidation_shouldFail() {
        Account account = activeAccount();
        PhysicalCard physicalCard = activePhysicalUndelivered(account);

        UUID accountId = account.id();

        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));
        when(physicalCardRepository.findActiveByAccountId(accountId)).thenReturn(Optional.of(physicalCard));

        IssueVirtualCardUseCase useCase = new IssueVirtualCardUseCase(
                accountRepository, physicalCardRepository, virtualCardRepository, processorPort, CLOCK);

        ConflictException ex = Assertions.assertThrows(ConflictException.class, () -> useCase.issue(accountId));
        Assertions.assertEquals(ErrorCode.PHYSICAL_CARD_NOT_DELIVERED, ex.errorCode());
        verifyNoInteractions(processorPort);
        verify(virtualCardRepository, never()).findActiveByAccountId(accountId);
        verify(virtualCardRepository, never()).persist(any(VirtualCard.class));
    }

    @Test
    void issue_whenDeliveredButNotValidated_shouldFail() {
        Account account = activeAccount();
        PhysicalCard physical = activePhysicalDeliveredNotValidated(account);

        when(accountRepository.findById(account.id())).thenReturn(Optional.of(account));
        when(physicalCardRepository.findActiveByAccountId(account.id())).thenReturn(Optional.of(physical));

        IssueVirtualCardUseCase useCase = new IssueVirtualCardUseCase(
                accountRepository, physicalCardRepository, virtualCardRepository, processorPort, CLOCK);

        ConflictException ex = Assertions.assertThrows(ConflictException.class, () -> useCase.issue(account.id()));
        Assertions.assertEquals(ErrorCode.PHYSICAL_CARD_NOT_VALIDATED, ex.errorCode());
        verifyNoInteractions(processorPort);
    }

    @Test
    void issue_whenVirtualAlreadyActive_shouldFail() {
        Account account = activeAccount();
        PhysicalCard physical = activePhysicalDeliveredValidated(account);

        VirtualCard existing = activeVirtual(account, "pc_existing");

        when(accountRepository.findById(account.id())).thenReturn(Optional.of(account));
        when(physicalCardRepository.findActiveByAccountId(account.id())).thenReturn(Optional.of(physical));
        when(virtualCardRepository.findActiveByAccountId(account.id())).thenReturn(Optional.of(existing));

        IssueVirtualCardUseCase useCase = new IssueVirtualCardUseCase(
                accountRepository, physicalCardRepository, virtualCardRepository, processorPort, CLOCK);

        ConflictException ex = Assertions.assertThrows(ConflictException.class, () -> useCase.issue(account.id()));
        Assertions.assertEquals(ErrorCode.VIRTUAL_CARD_ALREADY_EXISTS, ex.errorCode());
        verifyNoInteractions(processorPort);
    }

    @Test
    void issueVirtualCard_afterDeliveryAndValidation_shouldSucceed() {
        Account account = activeAccount();
        PhysicalCard physicalCard = activePhysicalDeliveredValidated(account);

        UUID accountId = account.id();

        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));
        when(physicalCardRepository.findActiveByAccountId(accountId)).thenReturn(Optional.of(physicalCard));
        when(virtualCardRepository.findActiveByAccountId(accountId)).thenReturn(Optional.empty());
        when(processorPort.issueVirtualCard(accountId.toString()))
                .thenReturn(new ProcessorPort.IssuedVirtualCard(accountId.toString(), "pc_abc", LocalDateTime.of(2026, 1, 31, 12, 15, 0)));

        IssueVirtualCardUseCase useCase = new IssueVirtualCardUseCase(
                accountRepository, physicalCardRepository, virtualCardRepository, processorPort, CLOCK);

        var response = useCase.issue(accountId);

        Assertions.assertEquals("pc_abc", response.processorCardId());
        Assertions.assertNotNull(response.virtualCardId());

        ArgumentCaptor<VirtualCard> captor = ArgumentCaptor.forClass(VirtualCard.class);
        verify(virtualCardRepository).persist(captor.capture());
        VirtualCard persisted = captor.getValue();
        Assertions.assertEquals("pc_abc", persisted.processorCardId());
        Assertions.assertEquals(accountId.toString(), persisted.processorAccountId());
        verify(processorPort).issueVirtualCard(accountId.toString());
    }
}
