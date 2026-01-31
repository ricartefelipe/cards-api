package com.altbank.cardsapi.application.usecase;

import com.altbank.cardsapi.application.exception.ConflictException;
import com.altbank.cardsapi.application.exception.ErrorCode;
import com.altbank.cardsapi.application.port.AccountRepository;
import com.altbank.cardsapi.application.port.PhysicalCardRepository;
import com.altbank.cardsapi.application.port.ProcessorPort;
import com.altbank.cardsapi.application.port.VirtualCardRepository;
import com.altbank.cardsapi.domain.model.Account;
import com.altbank.cardsapi.domain.model.Address;
import com.altbank.cardsapi.domain.model.Customer;
import com.altbank.cardsapi.domain.model.DeliveryStatus;
import com.altbank.cardsapi.domain.model.PhysicalCard;
import com.altbank.cardsapi.domain.model.VirtualCard;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.Mockito.*;

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

    private final Clock clock = Clock.fixed(Instant.parse("2026-01-31T12:00:00Z"), ZoneOffset.UTC);

    @Test
    void issueVirtualCard_beforeDeliveryAndValidation_shouldFail() {
        Account account = sampleAccount();
        PhysicalCard physicalCard = new PhysicalCard(
                account,
                "trk_123",
                DeliveryStatus.SHIPPED,
                account.customer().address().asSingleLine(),
                null,
                null,
                clock
        );

        UUID accountId = account.id();

        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));
        when(physicalCardRepository.findActiveByAccountId(accountId)).thenReturn(Optional.of(physicalCard));
        when(virtualCardRepository.findActiveByAccountId(accountId)).thenReturn(Optional.empty());

        IssueVirtualCardUseCase useCase = new IssueVirtualCardUseCase(
                accountRepository,
                physicalCardRepository,
                virtualCardRepository,
                processorPort,
                clock
        );

        ConflictException ex = Assertions.assertThrows(ConflictException.class, () -> useCase.issue(accountId));
        Assertions.assertEquals(ErrorCode.PHYSICAL_CARD_NOT_DELIVERED, ex.errorCode());
        verifyNoInteractions(processorPort);
        verify(virtualCardRepository, never()).persist(any(VirtualCard.class));
    }

    @Test
    void issueVirtualCard_afterDeliveryAndValidation_shouldSucceed() {
        Account account = sampleAccount();
        PhysicalCard physicalCard = new PhysicalCard(
                account,
                "trk_123",
                DeliveryStatus.SHIPPED,
                account.customer().address().asSingleLine(),
                null,
                null,
                clock
        );
        physicalCard.applyCarrierDeliveryUpdate(
                DeliveryStatus.DELIVERED,
                LocalDateTime.of(2026, 1, 31, 12, 0, 0),
                null,
                account.customer().address().asSingleLine(),
                clock
        );
        physicalCard.validate(clock);

        UUID accountId = account.id();

        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));
        when(physicalCardRepository.findActiveByAccountId(accountId)).thenReturn(Optional.of(physicalCard));
        when(virtualCardRepository.findActiveByAccountId(accountId)).thenReturn(Optional.empty());
        when(processorPort.issueVirtualCard(accountId.toString()))
                .thenReturn(new ProcessorPort.IssuedVirtualCard(accountId.toString(), "pc_abc", LocalDateTime.of(2026, 1, 31, 12, 15, 0)));

        IssueVirtualCardUseCase useCase = new IssueVirtualCardUseCase(
                accountRepository,
                physicalCardRepository,
                virtualCardRepository,
                processorPort,
                clock
        );

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

    private Account sampleAccount() {
        Address address = new Address("Rua A", "100", "Fortaleza", "CE", "60000-000", "BR");
        Customer customer = new Customer("Fulano", "12345678900", "fulano@example.com", null, address, clock);
        return new Account(customer, clock);
    }
}
