package com.altbank.cardsapi.application.usecase;

import jakarta.inject.Inject;
import com.altbank.cardsapi.application.dto.ProcessorCvvRotationWebhookRequest;
import com.altbank.cardsapi.application.dto.WebhookAckResponse;
import com.altbank.cardsapi.application.exception.BadRequestException;
import com.altbank.cardsapi.application.exception.ErrorCode;
import com.altbank.cardsapi.application.exception.NotFoundException;
import com.altbank.cardsapi.application.port.ProcessorPort;
import com.altbank.cardsapi.application.port.VirtualCardRepository;
import com.altbank.cardsapi.domain.model.VirtualCard;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Objects;

@ApplicationScoped
public class HandleProcessorCvvRotationWebhookUseCase {

    private final VirtualCardRepository virtualCardRepository;
    private final ProcessorPort processorPort;
    private final Clock clock;

    @Inject

    public HandleProcessorCvvRotationWebhookUseCase(VirtualCardRepository virtualCardRepository, ProcessorPort processorPort, Clock clock) {
        this.virtualCardRepository = Objects.requireNonNull(virtualCardRepository, "virtualCardRepository");
        this.processorPort = Objects.requireNonNull(processorPort, "processorPort");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    @Transactional
    public WebhookAckResponse handle(ProcessorCvvRotationWebhookRequest request) {
        VirtualCard card = virtualCardRepository.findByProcessorCardId(request.cardId())
                .orElseThrow(() -> new NotFoundException(ErrorCode.VIRTUAL_CARD_NOT_FOUND, "Virtual card not found for processor card id"));

        if (!card.processorAccountId().equals(request.accountId())) {
            throw new BadRequestException(ErrorCode.CARD_NOT_FOUND, "Account and card mismatch in processor webhook");
        }

        card.updateCvvExpirationAt(request.expirationDate());
        processorPort.acceptCvvRotationWebhook(request.accountId(), request.cardId(), request.nextCvv(), request.expirationDate());

        return new WebhookAckResponse("OK", LocalDateTime.now(clock));
    }
}
