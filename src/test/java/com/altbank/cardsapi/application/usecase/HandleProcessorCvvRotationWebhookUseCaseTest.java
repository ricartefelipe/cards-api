package com.altbank.cardsapi.application.usecase;

import com.altbank.cardsapi.application.dto.ProcessorCvvRotationWebhookRequest;
import com.altbank.cardsapi.application.dto.WebhookAckResponse;
import com.altbank.cardsapi.application.exception.BadRequestException;
import com.altbank.cardsapi.application.exception.ErrorCode;
import com.altbank.cardsapi.application.exception.NotFoundException;
import com.altbank.cardsapi.application.port.ProcessorPort;
import com.altbank.cardsapi.application.port.VirtualCardRepository;
import com.altbank.cardsapi.domain.model.Account;
import com.altbank.cardsapi.domain.model.VirtualCard;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.altbank.cardsapi.CardsTestFixtures.CLOCK;
import static com.altbank.cardsapi.CardsTestFixtures.activeAccount;
import static com.altbank.cardsapi.CardsTestFixtures.activeVirtual;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HandleProcessorCvvRotationWebhookUseCaseTest {

    @Mock
    VirtualCardRepository virtualCardRepository;

    @Mock
    ProcessorPort processorPort;

    @Test
    void handle_whenCardUnknown_shouldFail() {
        when(virtualCardRepository.findByProcessorCardId("pc_x")).thenReturn(Optional.empty());

        HandleProcessorCvvRotationWebhookUseCase useCase =
                new HandleProcessorCvvRotationWebhookUseCase(virtualCardRepository, processorPort, CLOCK);

        ProcessorCvvRotationWebhookRequest req = new ProcessorCvvRotationWebhookRequest(
                "acc", "pc_x", 111, LocalDateTime.of(2026, 2, 1, 12, 0));

        NotFoundException ex = Assertions.assertThrows(NotFoundException.class, () -> useCase.handle(req));
        Assertions.assertEquals(ErrorCode.VIRTUAL_CARD_NOT_FOUND, ex.errorCode());
        verifyNoInteractions(processorPort);
    }

    @Test
    void handle_whenAccountMismatch_shouldFail() {
        Account account = activeAccount();
        VirtualCard card = activeVirtual(account, "pc_m");

        when(virtualCardRepository.findByProcessorCardId("pc_m")).thenReturn(Optional.of(card));

        HandleProcessorCvvRotationWebhookUseCase useCase =
                new HandleProcessorCvvRotationWebhookUseCase(virtualCardRepository, processorPort, CLOCK);

        ProcessorCvvRotationWebhookRequest req = new ProcessorCvvRotationWebhookRequest(
                "wrong-account", "pc_m", 222, LocalDateTime.of(2026, 2, 1, 12, 0));

        BadRequestException ex = Assertions.assertThrows(BadRequestException.class, () -> useCase.handle(req));
        Assertions.assertEquals(ErrorCode.CARD_NOT_FOUND, ex.errorCode());
        verifyNoInteractions(processorPort);
    }

    @Test
    void handle_whenValid_shouldUpdateProcessorState() {
        Account account = activeAccount();
        VirtualCard card = activeVirtual(account, "pc_ok");
        String accId = card.processorAccountId();

        when(virtualCardRepository.findByProcessorCardId("pc_ok")).thenReturn(Optional.of(card));

        HandleProcessorCvvRotationWebhookUseCase useCase =
                new HandleProcessorCvvRotationWebhookUseCase(virtualCardRepository, processorPort, CLOCK);

        LocalDateTime exp = LocalDateTime.of(2026, 2, 2, 9, 0);
        ProcessorCvvRotationWebhookRequest req = new ProcessorCvvRotationWebhookRequest(accId, "pc_ok", 999, exp);

        WebhookAckResponse ack = useCase.handle(req);

        Assertions.assertEquals("OK", ack.status());
        Assertions.assertEquals(exp, card.cvvExpirationAt());
        verify(processorPort).acceptCvvRotationWebhook(eq(accId), eq("pc_ok"), eq(999), eq(exp));
    }
}
