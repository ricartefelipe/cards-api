package com.altbank.cardsapi.it;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Clock;
import java.util.UUID;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;

import com.altbank.cardsapi.application.exception.ConflictException;
import com.altbank.cardsapi.application.exception.ErrorCode;
import com.altbank.cardsapi.application.port.PhysicalCardRepository;
import com.altbank.cardsapi.domain.model.DeliveryStatus;
import com.altbank.cardsapi.domain.model.PhysicalCard;
import com.altbank.cardsapi.domain.model.ReissueReason;

/**
 * A restricao unica do predecessor, contra o banco real dos Dev Services.
 *
 * <p>Prova que um cartao nao pode ser predecessor de dois outros e que a violacao chega a aplicacao
 * como conflito (409), e nao como falha de persistencia nao tratada (500). A corrida por HTTP esta
 * em {@link ReissueConcurrencyIntegrationTest}; aqui o cenario e deterministico.
 */
@QuarkusTest
@Tag("integration")
class ReissueUniquenessIntegrationTest {

    @Inject
    PhysicalCardRepository physicalCards;

    @Inject
    Clock clock;

    @Test
    @TestSecurity(user = "admin", roles = {"admin", "user"})
    void aCardCanBeThePredecessorOfOnlyOneOtherCard() {
        UUID originalId = UUID.fromString(given().contentType(ContentType.JSON)
                .body("""
                    {
                      "customer": {"full_name":"Carla","document":"99988877700","email":"carla@example.com","phone":null,"subject":"carla-subject-1"},
                      "address": {"street":"Rua C","number":"300","city":"Fortaleza","state":"CE","zip_code":"60000-222","country":"BR"}
                    }
                    """)
                .when().post("/accounts")
                .then().statusCode(201)
                .body("physical_card_id", notNullValue())
                .extract().path("physical_card_id"));

        QuarkusTransaction.requiringNew().run(() -> physicalCards.persist(successorOf(originalId, "TRK-FIRST")));

        ConflictException conflict = assertThrows(
                ConflictException.class,
                () -> QuarkusTransaction.requiringNew()
                        .run(() -> physicalCards.persist(successorOf(originalId, "TRK-SECOND"))));

        assertEquals(ErrorCode.PHYSICAL_CARD_ALREADY_REISSUED, conflict.errorCode());
        assertEquals(409, conflict.httpStatus());
    }

    private PhysicalCard successorOf(UUID originalId, String trackingId) {
        PhysicalCard original = physicalCards.findById(originalId).orElseThrow();
        return new PhysicalCard(
                original.account(),
                trackingId,
                DeliveryStatus.IN_TRANSIT,
                "Rua C, 300",
                ReissueReason.THEFT,
                original,
                clock);
    }

}
