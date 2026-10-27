package com.altbank.cardsapi.application.usecase;

import com.altbank.cardsapi.application.dto.ProcessorCvvRotationWebhookRequest;
import com.altbank.cardsapi.application.dto.WebhookAckResponse;
import com.altbank.cardsapi.application.exception.BadRequestException;
import com.altbank.cardsapi.application.exception.ErrorCode;
import com.altbank.cardsapi.application.exception.NotFoundException;
import com.altbank.cardsapi.application.port.ProcessorPort;
import com.altbank.cardsapi.application.port.VirtualCardRepository;
import com.altbank.cardsapi.application.port.WebhookEventRepository;
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
import static org.mockito.ArgumentMatchers.any;
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

    @Mock
    WebhookEventRepository webhookEventRepository;

    @Test
    void handle_whenCardUnknown_shouldFail() {
        when(virtualCardRepository.findByProcessorCardId("pc_x")).thenReturn(Optional.empty());

        HandleProcessorCvvRotationWebhookUseCase useCase =
                new HandleProcessorCvvRotationWebhookUseCase(virtualCardRepository, processorPort, webhookEventRepository, CLOCK);

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
                new HandleProcessorCvvRotationWebhookUseCase(virtualCardRepository, processorPort, webhookEventRepository, CLOCK);

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
        when(webhookEventRepository.recordIfAbsent(any(), any())).thenReturn(true);

        HandleProcessorCvvRotationWebhookUseCase useCase =
                new HandleProcessorCvvRotationWebhookUseCase(virtualCardRepository, processorPort, webhookEventRepository, CLOCK);

        LocalDateTime exp = LocalDateTime.of(2026, 2, 2, 9, 0);
        ProcessorCvvRotationWebhookRequest req = new ProcessorCvvRotationWebhookRequest(accId, "pc_ok", 999, exp);

        WebhookAckResponse ack = useCase.handle(req);

        Assertions.assertEquals("OK", ack.status());
        Assertions.assertEquals(exp, card.cvvExpirationAt());
        verify(processorPort).acceptCvvRotationWebhook(eq(accId), eq("pc_ok"), eq(999), eq(exp));
    }

    @Test
    void handle_whenRotationIsNotNewer_shouldAckWithoutReapplying() {
        Account account = activeAccount();
        VirtualCard card = activeVirtual(account, "pc_stale");
        String accId = card.processorAccountId();
        LocalDateTime currentExpiration = card.cvvExpirationAt();

        when(virtualCardRepository.findByProcessorCardId("pc_stale")).thenReturn(Optional.of(card));
        when(webhookEventRepository.recordIfAbsent(any(), any())).thenReturn(true);

        HandleProcessorCvvRotationWebhookUseCase useCase =
                new HandleProcessorCvvRotationWebhookUseCase(virtualCardRepository, processorPort, webhookEventRepository, CLOCK);

        // Mesma expiracao atual (evento repetido/antigo): nao deve reaplicar nem regredir.
        ProcessorCvvRotationWebhookRequest req =
                new ProcessorCvvRotationWebhookRequest(accId, "pc_stale", 123, currentExpiration);

        WebhookAckResponse ack = useCase.handle(req);

        Assertions.assertEquals("OK", ack.status());
        Assertions.assertEquals(currentExpiration, card.cvvExpirationAt());
        verifyNoInteractions(processorPort);
    }

    @Test
    void handle_whenEventAlreadyPersisted_isIdempotentNoOp() {
        Account account = activeAccount();
        VirtualCard card = activeVirtual(account, "pc_dup");
        String accId = card.processorAccountId();
        LocalDateTime currentExpiration = card.cvvExpirationAt();

        when(virtualCardRepository.findByProcessorCardId("pc_dup")).thenReturn(Optional.of(card));
        when(webhookEventRepository.recordIfAbsent(any(), any())).thenReturn(false);

        HandleProcessorCvvRotationWebhookUseCase useCase =
                new HandleProcessorCvvRotationWebhookUseCase(virtualCardRepository, processorPort, webhookEventRepository, CLOCK);

        // Mesmo evento ja processado antes (sobrevive a restart/instancias): sem efeito.
        ProcessorCvvRotationWebhookRequest req =
                new ProcessorCvvRotationWebhookRequest(accId, "pc_dup", 777, LocalDateTime.of(2026, 3, 1, 10, 0));

        WebhookAckResponse ack = useCase.handle(req);

        Assertions.assertEquals("OK", ack.status());
        Assertions.assertEquals(currentExpiration, card.cvvExpirationAt());
        verifyNoInteractions(processorPort);
    }
}
