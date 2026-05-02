package com.altbank.cardsapi.infrastructure.external;

import com.altbank.cardsapi.application.exception.ErrorCode;
import com.altbank.cardsapi.application.exception.NotFoundException;
import com.altbank.cardsapi.application.port.ProcessorPort;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class InMemoryProcessorSimulatorTest {

    private static final Clock FIXED = Clock.fixed(Instant.parse("2026-06-01T10:00:00Z"), ZoneOffset.UTC);

    @Test
    void issue_get_rotate_flow() {

        InMemoryProcessorSimulator simulator = new InMemoryProcessorSimulator(3600L, FIXED);

        ProcessorPort.IssuedVirtualCard issued = simulator.issueVirtualCard("acct-1");

        ProcessorPort.CvvData cvv = simulator.getCurrentCvv(issued.processorCardId());

        Assertions.assertTrue(cvv.cvv() >= 100 && cvv.cvv() <= 999);

        LocalDateTime newExp = LocalDateTime.of(2027, 1, 1, 0, 0);

        simulator.acceptCvvRotationWebhook("acct-1", issued.processorCardId(), 777, newExp);

        Assertions.assertEquals(777, simulator.getCurrentCvv(issued.processorCardId()).cvv());
        Assertions.assertEquals(newExp, simulator.getCurrentCvv(issued.processorCardId()).expirationDate());
    }

    @Test
    void deactivate_removesCardFromStore() {

        InMemoryProcessorSimulator simulator = new InMemoryProcessorSimulator(3600L, FIXED);

        ProcessorPort.IssuedVirtualCard issued = simulator.issueVirtualCard("acct-2");

        simulator.getCurrentCvv(issued.processorCardId());

        simulator.deactivateCard(issued.processorCardId());

        NotFoundException ex = Assertions.assertThrows(NotFoundException.class, () -> simulator.getCurrentCvv(issued.processorCardId()));

        Assertions.assertEquals(ErrorCode.CVV_NOT_AVAILABLE, ex.errorCode());
    }

    @Test
    void getCurrentCvv_whenUnknownCard_shouldFail() {

        InMemoryProcessorSimulator simulator = new InMemoryProcessorSimulator(3600L, FIXED);

        NotFoundException ex =
                Assertions.assertThrows(NotFoundException.class, () -> simulator.getCurrentCvv("pc_unknown"));

        Assertions.assertEquals(ErrorCode.CVV_NOT_AVAILABLE, ex.errorCode());
    }
}
