package com.altbank.cardsapi.application.usecase;

import com.altbank.cardsapi.application.dto.ReissueVirtualCardRequest;
import com.altbank.cardsapi.application.exception.ErrorCode;
import com.altbank.cardsapi.application.exception.ForbiddenException;
import com.altbank.cardsapi.application.port.ProcessorPort;
import com.altbank.cardsapi.application.port.VirtualCardRepository;
import com.altbank.cardsapi.domain.model.Account;
import com.altbank.cardsapi.domain.model.ReissueReason;
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
import static com.altbank.cardsapi.CardsTestFixtures.activeVirtual;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReissueVirtualCardUseCaseTest {

    @Mock
    VirtualCardRepository virtualCardRepository;

    @Mock
    ProcessorPort processorPort;

    @Test
    void reissueVirtualCard_shouldDeactivateOldAndCreateNew() {
        Account account = activeAccount();
        String processorAccountId = account.id().toString();

        VirtualCard oldCard =
                activeVirtual(account, "pc_old");

        UUID oldCardId = oldCard.id();

        when(virtualCardRepository.findById(oldCardId)).thenReturn(Optional.of(oldCard));
        when(processorPort.issueVirtualCard(processorAccountId))
                .thenReturn(new ProcessorPort.IssuedVirtualCard(processorAccountId, "pc_new", LocalDateTime.of(2026, 1, 31, 12, 30, 0)));

        ReissueVirtualCardUseCase useCase = new ReissueVirtualCardUseCase(virtualCardRepository, processorPort, CLOCK);

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
        Account account = activeAccount();
        account.cancel(CLOCK);

        VirtualCard oldCard = activeVirtual(account, "pc_old");

        when(virtualCardRepository.findById(oldCard.id())).thenReturn(Optional.of(oldCard));

        ReissueVirtualCardUseCase useCase = new ReissueVirtualCardUseCase(virtualCardRepository, processorPort, CLOCK);

        ForbiddenException ex = Assertions.assertThrows(
                ForbiddenException.class,
                () -> useCase.reissue(oldCard.id(), new ReissueVirtualCardRequest(ReissueReason.LOSS)));

        Assertions.assertEquals(ErrorCode.ACCOUNT_CANCELLED, ex.errorCode());
        verifyNoInteractions(processorPort);
        verify(virtualCardRepository, never()).persist(any(VirtualCard.class));
    }

    @Test
    void reissueVirtualCard_whenInactive_shouldFail() {
        Account account = activeAccount();
        VirtualCard oldCard = activeVirtual(account, "pc_inactive");
        oldCard.deactivate(CLOCK);

        when(virtualCardRepository.findById(oldCard.id())).thenReturn(Optional.of(oldCard));

        ReissueVirtualCardUseCase useCase = new ReissueVirtualCardUseCase(virtualCardRepository, processorPort, CLOCK);

        ForbiddenException ex = Assertions.assertThrows(
                ForbiddenException.class,
                () -> useCase.reissue(oldCard.id(), new ReissueVirtualCardRequest(ReissueReason.DAMAGE)));

        Assertions.assertEquals(ErrorCode.CARD_INACTIVE, ex.errorCode());
        verifyNoInteractions(processorPort);
        verify(virtualCardRepository, never()).persist(any(VirtualCard.class));
    }
}
