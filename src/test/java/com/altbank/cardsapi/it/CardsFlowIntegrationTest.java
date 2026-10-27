package com.altbank.cardsapi.it;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.quarkus.test.security.jwt.Claim;
import io.quarkus.test.security.jwt.JwtSecurity;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

/**
 * Jornada integrada sobre HTTP com banco real (Dev Services) e identidade via @TestSecurity:
 * prova migracoes, persistencia, fluxo de emissao e autorizacao por objeto (IDOR) e o
 * cabecalho anti-cache do CVV. Ordenado: o admin cria os dados; os titulares consultam.
 */
@QuarkusTest
@Tag("integration")
@TestMethodOrder(OrderAnnotation.class)
class CardsFlowIntegrationTest {

    static final String ALICE = "alice-subject-1";
    static final String MALLORY = "mallory-subject-2";

    static String accountId;
    static String trackingId;
    static String physicalCardId;
    static String virtualCardId;

    @Test
    @Order(1)
    @TestSecurity(user = "admin", roles = {"admin", "user"})
    void adminProvisionsAndIssuesForAlice() {
        // 1) Cria conta vinculada ao subject de alice (cria cliente + cartao fisico + tracking).
        var create = given().contentType(ContentType.JSON)
                .body("""
                    {
                      "customer": {"full_name":"Alice","document":"11122233300","email":"alice@example.com","phone":null,"subject":"%s"},
                      "address": {"street":"Rua A","number":"100","city":"Fortaleza","state":"CE","zip_code":"60000-000","country":"BR"}
                    }
                    """.formatted(ALICE))
                .when().post("/accounts")
                .then().statusCode(201)
                .body("account_id", notNullValue())
                .extract();
        accountId = create.path("account_id");
        trackingId = create.path("tracking_id");
        physicalCardId = create.path("physical_card_id");

        // 2) Transportadora entrega (webhook por API key).
        given().contentType(ContentType.JSON)
                .header("X-Webhook-Api-Key", "carrier-local-key")
                .body("""
                    {"tracking_id":"%s","delivery_status":"DELIVERED","delivery_date":"2026-02-01T11:00:00","delivery_return_reason":null,"delivery_address":"Rua A, 100"}
                    """.formatted(trackingId))
                .when().post("/webhooks/carrier/delivery")
                .then().statusCode(200);

        // 3) Valida o cartao fisico e 4) emite o virtual.
        given().when().post("/physical-cards/" + physicalCardId + "/validate")
                .then().statusCode(200);

        virtualCardId = given().when().post("/accounts/" + accountId + "/virtual-cards")
                .then().statusCode(200)
                .body("virtual_card_id", notNullValue())
                .extract().path("virtual_card_id");
    }

    @Test
    @Order(2)
    @TestSecurity(user = "alice", roles = {"user"})
    @JwtSecurity(claims = @Claim(key = "sub", value = ALICE))
    void ownerReadsOwnCvvWithNoStoreHeader() {
        given().when().get("/virtual-cards/" + virtualCardId + "/cvv")
                .then().statusCode(200)
                .header("Cache-Control", "no-store")
                .body("cvv", notNullValue());

        given().when().get("/virtual-cards")
                .then().statusCode(200)
                .body("size()", equalTo(1))
                .body("[0].id", equalTo(virtualCardId));
    }

    @Test
    @Order(3)
    @TestSecurity(user = "mallory", roles = {"user"})
    @JwtSecurity(claims = @Claim(key = "sub", value = MALLORY))
    void otherUserCannotAccessAliceCard() {
        // Nao e dono: 404 (nao vaza existencia) no recurso e no CVV.
        given().when().get("/virtual-cards/" + virtualCardId)
                .then().statusCode(404);
        given().when().get("/virtual-cards/" + virtualCardId + "/cvv")
                .then().statusCode(404);
        // Lista vazia: mallory nao tem conta vinculada.
        given().when().get("/virtual-cards")
                .then().statusCode(200)
                .body("size()", equalTo(0));
    }

    @Test
    @Order(4)
    @TestSecurity(user = "alice", roles = {"user"})
    @JwtSecurity(claims = @Claim(key = "sub", value = ALICE))
    void reissueIsNotDuplicatedForTheSameCard() {
        // Primeira reemissao do cartao: cria um novo e desativa o antigo.
        given().contentType(ContentType.JSON).body("{\"reason\":\"THEFT\"}")
                .when().post("/virtual-cards/" + virtualCardId + "/reissue")
                .then().statusCode(200)
                .body("new_virtual_card_id", notNullValue());

        // Repetir a reemissao do MESMO cartao (ja inativo) nao gera outro cartao.
        given().contentType(ContentType.JSON).body("{\"reason\":\"THEFT\"}")
                .when().post("/virtual-cards/" + virtualCardId + "/reissue")
                .then().statusCode(403);
    }
}
