package com.altbank.cardsapi.infrastructure.external;

import com.altbank.cardsapi.application.port.CarrierPort;
import com.altbank.cardsapi.domain.model.DeliveryStatus;
import io.quarkus.arc.profile.UnlessBuildProfile;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Transportadora simulada para dev/test e demonstracao sintetica, com comportamento controlavel:
 * aceita por padrao, e pode recusar ou falhar a proxima chamada para exercitar a orquestracao.
 * Restrito a perfis diferentes de "prod" (ver ProductionReadinessGuard).
 */
@ApplicationScoped
@UnlessBuildProfile("prod")
public class InMemoryCarrierAdapter implements CarrierPort {

    private enum Next { REJECT, FAIL }

    private record Script(Next next, String message) {
    }

    private final Queue<Script> script = new ConcurrentLinkedQueue<>();
    private final AtomicInteger calls = new AtomicInteger();

    @Override
    public ShipmentOutcome createShipment(ShipmentOrder order) {
        calls.incrementAndGet();
        Script step = script.poll();
        if (step == null) {
            return new Accepted("trk_" + UUID.randomUUID(), DeliveryStatus.SHIPPED);
        }
        return switch (step.next()) {
            case REJECT -> new Rejected(step.message());
            case FAIL -> throw new IllegalStateException(step.message());
        };
    }

    public void rejectNext(String reason) {
        script.add(new Script(Next.REJECT, reason));
    }

    /** A proxima chamada sai sem resposta: o resultado fica desconhecido para quem chamou. */
    public void failNext(String message) {
        script.add(new Script(Next.FAIL, message));
    }

    public void reset() {
        script.clear();
        calls.set(0);
    }

    public int calls() {
        return calls.get();
    }
}
