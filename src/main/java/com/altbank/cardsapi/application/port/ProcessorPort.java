package com.altbank.cardsapi.application.port;

import java.time.LocalDateTime;

public interface ProcessorPort {

    IssuedVirtualCard issueVirtualCard(String processorAccountId);

    CvvData getCurrentCvv(String processorCardId);

    void acceptCvvRotationWebhook(String processorAccountId, String processorCardId, int nextCvv, LocalDateTime expirationDate);

    void deactivateCard(String processorCardId);

    record IssuedVirtualCard(String processorAccountId, String processorCardId, LocalDateTime cvvExpirationAt) {
    }

    record CvvData(int cvv, LocalDateTime expirationDate) {
    }
}
