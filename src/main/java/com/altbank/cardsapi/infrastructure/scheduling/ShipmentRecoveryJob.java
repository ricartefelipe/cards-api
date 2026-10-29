package com.altbank.cardsapi.infrastructure.scheduling;

import com.altbank.cardsapi.application.shipping.ShipmentOrchestrator;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.Duration;
import java.util.Objects;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Recuperacao de remessas: reenvia o que nunca saiu e encerra como incerto o que saiu e nao
 * voltou. Nao e retry do incerto — isso exigiria consulta ou cancelamento contratados.
 */
@ApplicationScoped
public class ShipmentRecoveryJob {

    static final String ACTOR = "shipment-recovery";
    private static final int BATCH = 50;

    private final ShipmentOrchestrator shipments;
    private final Duration grace;

    @Inject
    public ShipmentRecoveryJob(ShipmentOrchestrator shipments,
                               @ConfigProperty(name = "cards.shipping.in-progress-grace", defaultValue = "PT5M") Duration grace) {
        this.shipments = Objects.requireNonNull(shipments, "shipments");
        this.grace = Objects.requireNonNull(grace, "grace");
    }

    @Scheduled(every = "{cards.shipping.recovery-interval}", concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    void recover() {
        shipments.recoverPending(BATCH, ACTOR);
        shipments.settleStale(grace, BATCH, ACTOR);
    }
}
