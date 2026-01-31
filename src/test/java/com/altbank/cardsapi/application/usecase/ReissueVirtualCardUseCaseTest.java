package com.altbank.cardsapi.application.usecase;

import com.altbank.cardsapi.application.dto.ReissueVirtualCardRequest;
import com.altbank.cardsapi.application.exception.ErrorCode;
import com.altbank.cardsapi.application.exception.ForbiddenException;
import com.altbank.cardsapi.application.port.ProcessorPort;
import com.altbank.cardsapi.application.port.VirtualCardRepository;
import com.altbank.cardsapi.domain.model.Account;
import com.altbank.cardsapi.domain.model.Address;
import com.altbank.cardsapi.domain.model.Customer;
import com.altbank.cardsapi.domain.model.ReissueReason;
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
class ReissueVirtualCardUseCaseTest {

    @Mock
    VirtualCardRepository virtualCardRepository;

    @Mock
    ProcessorPort processorPort;

    private final Clock clock = Clock.fixed(Instant.parse("2026-01-31T12:00:00Z"), ZoneOffset.UTC);

    @Test
    void reissueVirtualCard_shouldDeactivateOldAndCreateNew() {
        Account account = sampleAccount();
        String processorAccountId = account.id().toString();

        VirtualCard oldCard = new VirtualCard(
                account,
                processorAccountId,
                "pc_old",
                LocalDateTime.of(2026, 1, 31, 12, 10, 0),
                clock
        );

        UUID oldCardId = oldCard.id();

        when(virtualCardRepository.findById(oldCardId)).thenReturn(Optional.of(oldCard));
        when(processorPort.issueVirtualCard(processorAccountId))
                .thenReturn(new ProcessorPort.IssuedVirtualCard(processorAccountId, "pc_new", LocalDateTime.of(2026, 1, 31, 12, 30, 0)));

        ReissueVirtualCardUseCase useCase = new ReissueVirtualCardUseCase(virtualCardRepository, processorPort, clock);

        var response = useCase.reissue(oldCardId, new ReissueVirtualCardRequest(ReissueReason.THEFT));

        Assertions.assertEquals(oldCardId, response.oldVirtualCardId());
        Assertions.assertNotNull(response.newVirtualCardId());
        Assertions.assertEquals("pc_new", response.processorCardId());
        Assertions.assertEquals(LocalDateTime.of(2026, 1, 31, 12, 30, 0), response.cvvExpirationAt());
        Assertions.assertFalse(oldCard.isActive());

        verify(processorPort).deactivateCard("pc_old");
        verify(processorPort).issueVirtualCard(processorAccountId);

        ArgumentCaptor<VirtualCard> captor = ArgumentCaptor.forClass(VirtualCard.class);
        verify(virtualCardRepository).persist(captor.capture());

        VirtualCard persisted = captor.getValue();
        Assertions.assertEquals("pc_new", persisted.processorCardId());
        Assertions.assertEquals(processorAccountId, persisted.processorAccountId());
        Assertions.assertEquals(ReissueReason.THEFT, persisted.reissueReason());
        Assertions.assertEquals(oldCardId.toString(), persisted.previousVirtualCardId());
        Assertions.assertTrue(persisted.isActive());
    }

    @Test
    void reissueVirtualCard_whenAccountCancelled_shouldFail() {
        Account account = sampleAccount();
        account.cancel(clock);

        VirtualCard oldCard = new VirtualCard(
                account,
                account.id().toString(),
                "pc_old",
                LocalDateTime.of(2026, 1, 31, 12, 10, 0),
                clock
        );

        when(virtualCardRepository.findById(oldCard.id())).thenReturn(Optional.of(oldCard));

        ReissueVirtualCardUseCase useCase = new ReissueVirtualCardUseCase(virtualCardRepository, processorPort, clock);

        ForbiddenException ex = Assertions.assertThrows(ForbiddenException.class,
                () -> useCase.reissue(oldCard.id(), new ReissueVirtualCardRequest(ReissueReason.LOSS)));
        Assertions.assertEquals(ErrorCode.ACCOUNT_CANCELLED, ex.errorCode());

        verifyNoInteractions(processorPort);
        verify(virtualCardRepository, never()).persist(any(VirtualCard.class));
    }

    private Account sampleAccount() {
        Address address = new Address("Rua A", "100", "Fortaleza", "CE", "60000-000", "BR");
        Customer customer = new Customer("Fulano", "12345678900", "fulano@example.com", null, address, clock);
        return new Account(customer, clock);
    }
}
