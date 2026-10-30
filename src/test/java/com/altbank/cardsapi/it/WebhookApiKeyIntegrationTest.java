package com.altbank.cardsapi.it;

import com.altbank.cardsapi.interfaces.rest.WebhookApiKeyFilter;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

/**
 * Os webhooks só podem ser aceitos com a chave configurada: sobre HTTP real, o caminho chega ao
 * filtro com barra inicial, e é isso que este teste garante (o unitário cobre o filtro isolado).
 */
@QuarkusTest
@Tag("integration")
class WebhookApiKeyIntegrationTest {

    private static final String CARRIER_BODY = """
        {"tracking_id":"trk_inexistente","delivery_status":"DELIVERED","delivery_date":"2026-02-01T11:00:00","delivery_return_reason":null,"delivery_address":"Rua A, 100"}
        """;
    private static final String PROCESSOR_BODY = """
        {"account_id":"acc","card_id":"card","next_cvv":123,"expiration_date":"2026-02-01T11:30:00"}
        """;

    @Test
    void carrierWebhook_withWrongKey_isRejectedBeforeProcessing() {
        given().contentType(ContentType.JSON)
                .header(WebhookApiKeyFilter.HEADER_NAME, "chave-errada")
                .body(CARRIER_BODY)
                .when().post("/webhooks/carrier/delivery")
                .then().statusCode(401)
                .body("error_code", equalTo("WEBHOOK_UNAUTHORIZED"));
    }

    @Test
    void carrierWebhook_withoutKey_isRejected() {
        given().contentType(ContentType.JSON)
                .body(CARRIER_BODY)
                .when().post("/webhooks/carrier/delivery")
                .then().statusCode(401);
    }

    @Test
    void processorWebhook_withWrongKey_isRejected() {
        given().contentType(ContentType.JSON)
                .header(WebhookApiKeyFilter.HEADER_NAME, "chave-errada")
                .body(PROCESSOR_BODY)
                .when().post("/webhooks/processor/cvv-rotation")
                .then().statusCode(401);
    }

}
