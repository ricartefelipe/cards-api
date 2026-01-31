package com.altbank.cardsapi.application.usecase;

import jakarta.inject.Inject;
import com.altbank.cardsapi.application.dto.CvvResponse;
import com.altbank.cardsapi.application.exception.ErrorCode;
import com.altbank.cardsapi.application.exception.ForbiddenException;
import com.altbank.cardsapi.application.exception.NotFoundException;
import com.altbank.cardsapi.application.port.ProcessorPort;
import com.altbank.cardsapi.application.port.VirtualCardRepository;
import com.altbank.cardsapi.domain.model.Account;
import com.altbank.cardsapi.domain.model.VirtualCard;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.util.Objects;
import java.util.UUID;

@ApplicationScoped
public class GetVirtualCardCvvUseCase {

    private final VirtualCardRepository virtualCardRepository;
    private final ProcessorPort processorPort;

    @Inject

    public GetVirtualCardCvvUseCase(VirtualCardRepository virtualCardRepository, ProcessorPort processorPort) {
        this.virtualCardRepository = Objects.requireNonNull(virtualCardRepository, "virtualCardRepository");
        this.processorPort = Objects.requireNonNull(processorPort, "processorPort");
    }

    @Transactional
    public CvvResponse get(UUID virtualCardId) {
        VirtualCard card = virtualCardRepository.findById(virtualCardId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.VIRTUAL_CARD_NOT_FOUND, "Virtual card not found"));

        Account account = card.account();
        if (!account.isActive()) {
            throw new ForbiddenException(ErrorCode.ACCOUNT_CANCELLED, "Account is cancelled");
        }
        if (!card.isActive()) {
            throw new ForbiddenException(ErrorCode.CARD_INACTIVE, "Card is inactive");
        }

        ProcessorPort.CvvData cvvData = processorPort.getCurrentCvv(card.processorCardId());
        return new CvvResponse(cvvData.cvv(), cvvData.expirationDate());
    }
}
