package com.altbank.cardsapi.application.shipping;

import com.altbank.cardsapi.application.audit.AuditAction;
import com.altbank.cardsapi.application.port.AuditRepository;
import com.altbank.cardsapi.application.port.CarrierPort;
import com.altbank.cardsapi.application.port.CarrierPort.Accepted;
import com.altbank.cardsapi.application.port.CarrierPort.Rejected;
import com.altbank.cardsapi.application.port.CarrierPort.ShipmentOrder;
import com.altbank.cardsapi.application.port.CarrierPort.ShipmentOutcome;
import com.altbank.cardsapi.application.port.PhysicalCardRepository;
import com.altbank.cardsapi.application.port.ShipmentRequestRepository;
import com.altbank.cardsapi.application.port.TransactionRunner;
import com.altbank.cardsapi.domain.model.PhysicalCard;
import com.altbank.cardsapi.domain.model.ShipmentRequest;
import com.altbank.cardsapi.domain.model.ShipmentStatus;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.jboss.logging.Logger;

/**
 * Leva um pedido de remessa do registro duravel ate a transportadora, sem segurar transacao nem
 * lock durante a chamada.
 *
 * <pre>
 *   tx1: PENDING -> IN_PROGRESS (reivindica; so um chamador passa)
 *   chamada a transportadora, fora de transacao
 *   tx2: IN_PROGRESS -> CONFIRMED | REJECTED | UNCERTAIN (so se ainda IN_PROGRESS)
 * </pre>
 *
 * Queda antes de tx1: o pedido segue PENDING e a recuperacao o reenvia. Queda entre tx1 e tx2:
 * o pedido fica IN_PROGRESS sem resposta; passado o prazo, vira UNCERTAIN e nao e reenviado,
 * porque a transportadora pode ter aceito.
 */
@ApplicationScoped
public class ShipmentOrchestrator {

    private static final Logger LOG = Logger.getLogger(ShipmentOrchestrator.class);

    /** Resultado de uma tentativa de despacho, do ponto de vista de quem chamou. */
    public record Dispatch(ShipmentStatus status, Optional<String> trackingId) {
    }

    private final ShipmentRequestRepository requests;
    private final PhysicalCardRepository cards;
    private final CarrierPort carrier;
    private final TransactionRunner tx;
    private final AuditRepository audit;
    private final Clock clock;

    @Inject
    public ShipmentOrchestrator(ShipmentRequestRepository requests,
                                PhysicalCardRepository cards,
                                CarrierPort carrier,
                                TransactionRunner tx,
                                AuditRepository audit,
                                Clock clock) {
        this.requests = Objects.requireNonNull(requests, "requests");
        this.cards = Objects.requireNonNull(cards, "cards");
        this.carrier = Objects.requireNonNull(carrier, "carrier");
        this.tx = Objects.requireNonNull(tx, "tx");
        this.audit = Objects.requireNonNull(audit, "audit");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    /** Registra a intencao na transacao de quem chama; nenhum efeito externo acontece aqui. */
    public UUID requestShipment(PhysicalCard card, String deliveryAddress, String actor) {
        ShipmentRequest request = new ShipmentRequest(card, deliveryAddress, clock);
        requests.persist(request);
        record(actor, AuditAction.SHIPMENT_REQUESTED, request);
        return request.id();
    }

    /** Tenta levar o pedido ate a transportadora. Pode ser chamado por varios ao mesmo tempo. */
    public Dispatch dispatch(UUID requestId, String actor) {
        Optional<ShipmentOrder> claimed = tx.inTransaction(() -> requests.findForUpdate(requestId)
                .filter(request -> request.start(clock))
                .map(request -> new ShipmentOrder(request.id(), request.deliveryAddress())));
        if (claimed.isEmpty()) {
            return current(requestId);
        }
        ShipmentOutcome outcome;
        String failure = null;
        try {
            outcome = carrier.createShipment(claimed.get());
        } catch (RuntimeException e) {
            // A chamada saiu e a resposta nao chegou: pode ter sido aceita. Nao reenviar.
            outcome = null;
            failure = e.getClass().getSimpleName() + ": " + e.getMessage();
            LOG.warnf("shipment %s: resultado desconhecido (%s)", requestId, failure);
        }
        return finish(requestId, outcome, failure, actor);
    }

    /** Pedidos que nunca chegaram a ser reivindicados (queda antes do envio). */
    public int recoverPending(int limit, String actor) {
        List<UUID> pending = tx.inTransaction(() -> requests.findByStatus(ShipmentStatus.PENDING, limit)
                .stream().map(ShipmentRequest::id).toList());
        pending.forEach(id -> dispatch(id, actor));
        return pending.size();
    }

    /** Pedidos reivindicados cujo chamador caiu antes de finalizar: viram UNCERTAIN, sem reenvio. */
    public int settleStale(Duration grace, int limit, String actor) {
        LocalDateTime cutoff = LocalDateTime.now(clock).minus(grace);
        List<UUID> stale = tx.inTransaction(() -> requests.findInProgressUpdatedBefore(cutoff, limit)
                .stream().map(ShipmentRequest::id).toList());
        for (UUID id : stale) {
            tx.inTransaction(() -> requests.findForUpdate(id)
                    .filter(request -> request.isStale(clock, grace))
                    .ifPresent(request -> {
                        request.uncertain("sem resposta apos " + grace, clock);
                        record(actor, AuditAction.SHIPMENT_UNCERTAIN, request);
                    }));
        }
        return stale.size();
    }

    private Dispatch finish(UUID requestId, ShipmentOutcome outcome, String failure, String actor) {
        return tx.inTransaction(() -> {
            ShipmentRequest request = requests.findForUpdate(requestId)
                    .orElseThrow(() -> new IllegalStateException("shipment request vanished: " + requestId));
            if (!request.isInProgress()) {
                // Outro caminho (ou a recuperacao) ja decidiu este pedido.
                return snapshot(request);
            }
            switch (outcome) {
                case Accepted accepted -> {
                    request.confirm(accepted.trackingId(), clock);
                    cards.findById(request.physicalCardId())
                            .ifPresent(card -> card.shipped(accepted.trackingId(), accepted.initialStatus()));
                    record(actor, AuditAction.SHIPMENT_CONFIRMED, request);
                }
                case Rejected rejected -> {
                    request.reject(rejected.reason(), clock);
                    record(actor, AuditAction.SHIPMENT_REJECTED, request);
                }
                case null -> {
                    request.uncertain(failure, clock);
                    record(actor, AuditAction.SHIPMENT_UNCERTAIN, request);
                }
            }
            return snapshot(request);
        });
    }

    private Dispatch current(UUID requestId) {
        return tx.inTransaction(() -> requests.findById(requestId).map(ShipmentOrchestrator::snapshot)
                .orElseThrow(() -> new IllegalStateException("shipment request not found: " + requestId)));
    }

    private static Dispatch snapshot(ShipmentRequest request) {
        return new Dispatch(request.status(), Optional.ofNullable(request.trackingId()));
    }

    private void record(String actor, AuditAction action, ShipmentRequest request) {
        audit.record(UUID.randomUUID().toString(), actor, action.name(), "shipment_request", request.id().toString());
    }
}
