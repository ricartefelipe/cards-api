package com.altbank.cardsapi.infrastructure.external;

import jakarta.inject.Inject;
import com.altbank.cardsapi.application.exception.ErrorCode;
import com.altbank.cardsapi.application.exception.NotFoundException;
import com.altbank.cardsapi.application.port.ProcessorPort;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public class InMemoryProcessorSimulator implements ProcessorPort {

    private final long defaultTtlSeconds;
    private final Clock clock;
    private final SecureRandom secureRandom = new SecureRandom();
    private final Map<String, Entry> cvvStore = new ConcurrentHashMap<>();

    @Inject

    public InMemoryProcessorSimulator(@ConfigProperty(name = "cards.processor.cvv.default-ttl-seconds") long defaultTtlSeconds,
                                      Clock clock) {
        this.defaultTtlSeconds = defaultTtlSeconds;
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    @Override
    public IssuedVirtualCard issueVirtualCard(String processorAccountId) {
        String processorCardId = "pc_" + UUID.randomUUID();
        int cvv = generateCvv();
        LocalDateTime expiration = LocalDateTime.now(clock).plusSeconds(defaultTtlSeconds);
        cvvStore.put(processorCardId, new Entry(cvv, expiration));
        return new IssuedVirtualCard(processorAccountId, processorCardId, expiration);
    }

    @Override
    public CvvData getCurrentCvv(String processorCardId) {
        Entry entry = cvvStore.get(processorCardId);
        if (entry == null) {
            throw new NotFoundException(ErrorCode.CVV_NOT_AVAILABLE, "CVV not available for card");
        }
        if (isExpired(entry.expirationDate())) {
            cvvStore.remove(processorCardId);
            throw new NotFoundException(ErrorCode.CVV_NOT_AVAILABLE, "CVV expired");
        }
        return new CvvData(entry.cvv(), entry.expirationDate());
    }

    @Override
    public void acceptCvvRotationWebhook(String processorAccountId, String processorCardId, int nextCvv, LocalDateTime expirationDate) {
        cvvStore.put(processorCardId, new Entry(nextCvv, expirationDate));
    }

    @Override
    public void deactivateCard(String processorCardId) {
        cvvStore.remove(processorCardId);
    }

    private boolean isExpired(LocalDateTime expirationDate) {
        if (expirationDate == null) {
            return false;
        }
        return expirationDate.toInstant(ZoneOffset.UTC).isBefore(clock.instant());
    }

    private int generateCvv() {
        return 100 + secureRandom.nextInt(900);
    }

    private record Entry(int cvv, LocalDateTime expirationDate) {
    }
}
