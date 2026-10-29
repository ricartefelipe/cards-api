package com.altbank.cardsapi.application.shipping;

import static com.altbank.cardsapi.CardsTestFixtures.CLOCK;
import static com.altbank.cardsapi.CardsTestFixtures.activeAccount;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.altbank.cardsapi.application.port.AuditRepository;
import com.altbank.cardsapi.application.port.CarrierPort;
import com.altbank.cardsapi.application.port.PhysicalCardRepository;
import com.altbank.cardsapi.application.port.ShipmentRequestRepository;
import com.altbank.cardsapi.application.port.TransactionRunner;
import com.altbank.cardsapi.domain.model.DeliveryStatus;
import com.altbank.cardsapi.domain.model.PhysicalCard;
import com.altbank.cardsapi.domain.model.ShipmentRequest;
import com.altbank.cardsapi.domain.model.ShipmentStatus;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;

class ShipmentOrchestratorTest {

    /** Repositorio em memoria: o bastante para a maquina de estados; o lock e provado no banco real. */
    static final class MemoryRequests implements ShipmentRequestRepository {
        final Map<UUID, ShipmentRequest> rows = new LinkedHashMap<>();

        @Override public Optional<ShipmentRequest> findById(UUID id) { return Optional.ofNullable(rows.get(id)); }
        @Override public Optional<ShipmentRequest> findForUpdate(UUID id) { return findById(id); }
        @Override public Optional<ShipmentRequest> findByPhysicalCardId(UUID cardId) {
            return rows.values().stream().filter(r -> r.physicalCardId().equals(cardId)).findFirst();
        }
        @Override public List<ShipmentRequest> findByStatus(ShipmentStatus status, int limit) {
            return rows.values().stream().filter(r -> r.status() == status).limit(limit).toList();
        }
        @Override public List<ShipmentRequest> findInProgressUpdatedBefore(LocalDateTime instant, int limit) {
            return rows.values().stream().filter(r -> r.isInProgress() && r.updatedAt().isBefore(instant)).limit(limit).toList();
        }
        @Override public void persist(ShipmentRequest request) { rows.put(request.id(), request); }
    }

    static final class MemoryCards implements PhysicalCardRepository {
        final Map<UUID, PhysicalCard> rows = new LinkedHashMap<>();
        @Override public Optional<PhysicalCard> findById(UUID id) { return Optional.ofNullable(rows.get(id)); }
        @Override public Optional<PhysicalCard> findByTrackingId(String t) { return Optional.empty(); }
        @Override public Optional<PhysicalCard> findActiveByAccountId(UUID a) { return Optional.empty(); }
        @Override public List<PhysicalCard> listAll(int o, int l) { return List.of(); }
        @Override public List<PhysicalCard> listByAccountId(UUID a) { return List.of(); }
        @Override public void persist(PhysicalCard card) { rows.put(card.id(), card); }
    }

    /** Transportadora programavel por chamada; conta quantas vezes foi acionada. */
    static final class ScriptedCarrier implements CarrierPort {
        final List<ShipmentOrder> calls = new ArrayList<>();
        Function<ShipmentOrder, ShipmentOutcome> behaviour = o -> new Accepted("trk_" + o.requestId(), DeliveryStatus.SHIPPED);
        @Override public ShipmentOutcome createShipment(ShipmentOrder order) {
            calls.add(order);
            return behaviour.apply(order);
        }
    }

    static final TransactionRunner INLINE = new TransactionRunner() {
        @Override public <T> T inTransaction(Supplier<T> work) { return work.get(); }
    };

    final MemoryRequests requests = new MemoryRequests();
    final MemoryCards cards = new MemoryCards();
    final ScriptedCarrier carrier = new ScriptedCarrier();
    final List<String> audit = new ArrayList<>();
    final AuditRepository auditRepository = (id, subject, action, type, target) -> audit.add(action);

    private ShipmentOrchestrator orchestrator(Clock clock) {
        return new ShipmentOrchestrator(requests, cards, carrier, INLINE, auditRepository, clock);
    }

    private PhysicalCard pendingCard() {
        PhysicalCard card = new PhysicalCard(activeAccount(), "Rua A, 1", null, null, CLOCK);
        cards.persist(card);
        return card;
    }

    @Test
    void acceptedShipmentConfirmsTheRequestAndGivesTheCardItsTracking() {
        ShipmentOrchestrator shipments = orchestrator(CLOCK);
        PhysicalCard card = pendingCard();
        UUID id = shipments.requestShipment(card, "Rua A, 1", "t");

        ShipmentOrchestrator.Dispatch result = shipments.dispatch(id, "t");

        assertEquals(ShipmentStatus.CONFIRMED, result.status());
        assertEquals("trk_" + id, result.trackingId().orElseThrow());
        assertEquals("trk_" + id, card.trackingId());
        assertEquals(DeliveryStatus.SHIPPED, card.deliveryStatus());
        assertEquals(List.of("SHIPMENT_REQUESTED", "SHIPMENT_CONFIRMED"), audit);
    }

    @Test
    void rejectedShipmentLeavesTheCardWithoutTracking() {
        ShipmentOrchestrator shipments = orchestrator(CLOCK);
        PhysicalCard card = pendingCard();
        carrier.behaviour = o -> new CarrierPort.Rejected("endereco invalido");
        UUID id = shipments.requestShipment(card, "Rua A, 1", "t");

        ShipmentOrchestrator.Dispatch result = shipments.dispatch(id, "t");

        assertEquals(ShipmentStatus.REJECTED, result.status());
        assertTrue(card.awaitsShipment());
        assertEquals("endereco invalido", requests.rows.get(id).lastError());
    }

    @Test
    void anUnknownOutcomeIsUncertainAndIsNeverResentAutomatically() {
        ShipmentOrchestrator shipments = orchestrator(CLOCK);
        PhysicalCard card = pendingCard();
        carrier.behaviour = o -> { throw new IllegalStateException("timeout"); };
        UUID id = shipments.requestShipment(card, "Rua A, 1", "t");

        assertEquals(ShipmentStatus.UNCERTAIN, shipments.dispatch(id, "t").status());
        carrier.behaviour = o -> new CarrierPort.Accepted("trk_tarde", DeliveryStatus.SHIPPED);
        assertEquals(ShipmentStatus.UNCERTAIN, shipments.dispatch(id, "t").status());
        shipments.recoverPending(10, "t");

        assertEquals(1, carrier.calls.size(), "resultado incerto nao gera reenvio");
        assertNull(card.trackingId());
        assertEquals(1, requests.rows.get(id).attempts());
    }

    @Test
    void aConfirmedRequestIsNotDispatchedAgain() {
        ShipmentOrchestrator shipments = orchestrator(CLOCK);
        UUID id = shipments.requestShipment(pendingCard(), "Rua A, 1", "t");
        shipments.dispatch(id, "t");

        shipments.dispatch(id, "t");
        shipments.recoverPending(10, "t");

        assertEquals(1, carrier.calls.size());
    }

    @Test
    void recoveryDispatchesOnlyWhatWasNeverClaimed() {
        ShipmentOrchestrator shipments = orchestrator(CLOCK);
        UUID neverSent = shipments.requestShipment(pendingCard(), "Rua A, 1", "t");
        UUID done = shipments.requestShipment(pendingCard(), "Rua B, 2", "t");
        shipments.dispatch(done, "t");

        int recovered = shipments.recoverPending(10, "t");

        assertEquals(1, recovered);
        assertEquals(ShipmentStatus.CONFIRMED, requests.rows.get(neverSent).status());
        assertEquals(2, carrier.calls.size());
    }

    @Test
    void aClaimedRequestWhoseCallerDiedBecomesUncertainAfterTheGraceAndIsNotResent() {
        ShipmentOrchestrator shipments = orchestrator(CLOCK);
        UUID id = shipments.requestShipment(pendingCard(), "Rua A, 1", "t");
        // Reivindicado e nunca finalizado: o chamador caiu entre a tx1 e a chamada ou depois dela.
        requests.rows.get(id).start(CLOCK);
        carrier.calls.clear();

        Clock later = Clock.fixed(Instant.parse("2026-01-31T12:10:00Z"), ZoneOffset.UTC);
        ShipmentOrchestrator laterShipments = orchestrator(later);
        assertEquals(0, laterShipments.settleStale(Duration.ofMinutes(15), 10, "r"), "dentro do prazo nada muda");
        assertEquals(1, laterShipments.settleStale(Duration.ofMinutes(5), 10, "r"));

        assertEquals(ShipmentStatus.UNCERTAIN, requests.rows.get(id).status());
        laterShipments.recoverPending(10, "r");
        assertEquals(0, carrier.calls.size(), "incerto nao e reenviado");
        assertTrue(audit.contains("SHIPMENT_UNCERTAIN"));
    }
}
