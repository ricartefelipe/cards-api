package com.altbank.cardsapi.it;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.altbank.cardsapi.application.port.PhysicalCardRepository;
import com.altbank.cardsapi.application.port.ShipmentRequestRepository;
import com.altbank.cardsapi.application.port.TransactionRunner;
import com.altbank.cardsapi.application.shipping.ShipmentOrchestrator;
import com.altbank.cardsapi.domain.model.PhysicalCard;
import com.altbank.cardsapi.domain.model.ShipmentRequest;
import com.altbank.cardsapi.domain.model.ShipmentStatus;
import com.altbank.cardsapi.infrastructure.external.InMemoryCarrierAdapter;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * A remessa como intencao duravel, contra o banco real: queda antes do envio, resultado incerto,
 * recusa, reentrega e dois despachantes disputando o mesmo pedido.
 */
@QuarkusTest
@Tag("integration")
class ShipmentOrchestrationIntegrationTest {

    @Inject InMemoryCarrierAdapter carrier;
    @Inject ShipmentOrchestrator shipments;
    @Inject ShipmentRequestRepository requests;
    @Inject PhysicalCardRepository cards;
    @Inject TransactionRunner tx;
    @Inject Clock clock;

    @BeforeEach
    void resetCarrier() {
        carrier.reset();
    }

    private static int seq = 700;

    private static String accountJson() {
        int n = seq++;
        return """
            {
              "customer": {"full_name":"Remessa %d","document":"%011d","email":"remessa%d@example.com","phone":null,"subject":"remessa-%d"},
              "address": {"street":"Rua R","number":"%d","city":"Fortaleza","state":"CE","zip_code":"60000-%03d","country":"BR"}
            }
            """.formatted(n, 30000000000L + n, n, n, n, n % 1000);
    }

    @Test
    @TestSecurity(user = "admin", roles = {"admin", "user"})
    void anUnknownCarrierOutcomeLeavesTheCardWithoutTrackingAndIsNotResent() {
        carrier.failNext("timeout na transportadora");

        var created = given().contentType(ContentType.JSON).body(accountJson())
                .when().post("/accounts")
                .then().statusCode(201)
                .body("tracking_id", nullValue())
                .body("shipment_status", org.hamcrest.Matchers.equalTo("UNCERTAIN"))
                .extract();
        UUID cardId = UUID.fromString(created.path("physical_card_id"));

        ShipmentRequest request = tx.inTransaction(() -> requests.findByPhysicalCardId(cardId).orElseThrow());
        assertEquals(ShipmentStatus.UNCERTAIN, request.status());
        assertEquals(1, request.attempts());
        assertTrue(tx.inTransaction(() -> cards.findById(cardId).orElseThrow().awaitsShipment()));

        shipments.dispatch(request.id(), "t");
        shipments.recoverPending(10, "t");
        assertEquals(1, carrier.calls(), "nenhum reenvio automatico de um resultado incerto");
    }

    @Test
    @TestSecurity(user = "admin", roles = {"admin", "user"})
    void aRejectedShipmentIsRecordedAndTheCardStaysWithoutTracking() {
        carrier.rejectNext("endereco nao atendido");

        var created = given().contentType(ContentType.JSON).body(accountJson())
                .when().post("/accounts")
                .then().statusCode(201)
                .body("shipment_status", org.hamcrest.Matchers.equalTo("REJECTED"))
                .extract();
        UUID cardId = UUID.fromString(created.path("physical_card_id"));

        ShipmentRequest request = tx.inTransaction(() -> requests.findByPhysicalCardId(cardId).orElseThrow());
        assertEquals("endereco nao atendido", request.lastError());
        given().when().get("/physical-cards/" + cardId).then().statusCode(200)
                .body("tracking_id", nullValue()).body("delivery_status", org.hamcrest.Matchers.equalTo("PENDING"));
    }

    @Test
    @TestSecurity(user = "admin", roles = {"admin", "user"})
    void aCrashBeforeTheCallIsRecoveredByDispatchingThePendingRequest() {
        UUID cardId = UUID.fromString(given().contentType(ContentType.JSON).body(accountJson())
                .when().post("/accounts").then().statusCode(201).extract().path("physical_card_id"));
        carrier.reset();
        // Simula a queda antes do envio: um cartao novo com intencao gravada e nunca despachada.
        UUID pending = tx.inTransaction(() -> {
            PhysicalCard previous = cards.findById(cardId).orElseThrow();
            PhysicalCard card = new PhysicalCard(previous.account(), "Rua R, 1", null, null, clock);
            cards.persist(card);
            return shipments.requestShipment(card, "Rua R, 1", "crash-test");
        });

        assertEquals(1, shipments.recoverPending(10, "recovery"));

        ShipmentRequest request = tx.inTransaction(() -> requests.findById(pending).orElseThrow());
        assertEquals(ShipmentStatus.CONFIRMED, request.status());
        assertEquals(1, carrier.calls());
        assertEquals(request.trackingId(), tx.inTransaction(() -> cards.findById(request.physicalCardId()).orElseThrow().trackingId()));
    }

    @Test
    @TestSecurity(user = "admin", roles = {"admin", "user"})
    void twoDispatchersRacingForTheSameRequestCallTheCarrierOnce() throws Exception {
        UUID cardId = UUID.fromString(given().contentType(ContentType.JSON).body(accountJson())
                .when().post("/accounts").then().statusCode(201).extract().path("physical_card_id"));
        carrier.reset();
        UUID pending = tx.inTransaction(() -> {
            PhysicalCard previous = cards.findById(cardId).orElseThrow();
            PhysicalCard card = new PhysicalCard(previous.account(), "Rua R, 2", null, null, clock);
            cards.persist(card);
            return shipments.requestShipment(card, "Rua R, 2", "race");
        });

        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Callable<ShipmentStatus> race = () -> { start.await(10, TimeUnit.SECONDS); return shipments.dispatch(pending, "race").status(); };
            Future<ShipmentStatus> a = pool.submit(race);
            Future<ShipmentStatus> b = pool.submit(race);
            start.countDown();
            List<ShipmentStatus> outcomes = List.of(a.get(60, TimeUnit.SECONDS), b.get(60, TimeUnit.SECONDS));
            // Quem perde a reivindicacao devolve o estado que encontrou — ainda IN_PROGRESS se o
            // vencedor nao finalizou, CONFIRMED se ja finalizou. O que nao pode e chamar de novo.
            assertTrue(outcomes.contains(ShipmentStatus.CONFIRMED), outcomes.toString());
            assertTrue(outcomes.stream().allMatch(o -> o == ShipmentStatus.CONFIRMED || o == ShipmentStatus.IN_PROGRESS), outcomes.toString());
        } finally {
            pool.shutdownNow();
        }
        assertEquals(1, carrier.calls(), "so quem reivindicou o pedido chama a transportadora");
        ShipmentRequest finalState = tx.inTransaction(() -> requests.findById(pending).orElseThrow());
        assertEquals(ShipmentStatus.CONFIRMED, finalState.status());
        assertEquals(1, finalState.attempts());
    }

    @Test
    @TestSecurity(user = "admin", roles = {"admin", "user"})
    void aClaimedRequestLeftBehindBecomesUncertainAndIsNotResent() {
        UUID cardId = UUID.fromString(given().contentType(ContentType.JSON).body(accountJson())
                .when().post("/accounts").then().statusCode(201).extract().path("physical_card_id"));
        carrier.reset();
        UUID left = tx.inTransaction(() -> {
            PhysicalCard previous = cards.findById(cardId).orElseThrow();
            PhysicalCard card = new PhysicalCard(previous.account(), "Rua R, 3", null, null, clock);
            cards.persist(card);
            UUID id = shipments.requestShipment(card, "Rua R, 3", "stale");
            requests.findForUpdate(id).orElseThrow().start(clock); // reivindicado, nunca finalizado
            return id;
        });

        assertEquals(0, shipments.settleStale(Duration.ofHours(1), 10, "recovery"), "dentro do prazo segue IN_PROGRESS");
        assertEquals(1, shipments.settleStale(Duration.ZERO, 10, "recovery"));

        assertEquals(ShipmentStatus.UNCERTAIN, tx.inTransaction(() -> requests.findById(left).orElseThrow().status()));
        shipments.recoverPending(10, "recovery");
        assertEquals(0, carrier.calls());
    }

    @Test
    @TestSecurity(user = "admin", roles = {"admin", "user"})
    void theHappyPathStillAnswersWithTracking() {
        given().contentType(ContentType.JSON).body(accountJson())
                .when().post("/accounts")
                .then().statusCode(201)
                .body("tracking_id", notNullValue())
                .body("shipment_status", org.hamcrest.Matchers.equalTo("CONFIRMED"));
    }
}
