package com.altbank.cardsapi.it;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;

/**
 * Duas reemissoes concorrentes do mesmo cartao, contra o banco real dos Dev Services.
 *
 * <p>A restricao unica sobre o predecessor (V6__reissue_idempotency) e a barreira de integridade.
 * O que se prova aqui e que a corrida termina com um unico cartao novo e que a requisicao perdedora
 * recebe 409 — e nao o 500 que uma violacao de restricao nao traduzida produziria.
 */
@QuarkusTest
@Tag("integration")
class ReissueConcurrencyIntegrationTest {

    private static final int REISSUERS = 2;

    @Test
    @TestSecurity(user = "admin", roles = {"admin", "user"})
    void twoConcurrentReissuesLeaveOneNewCardAndAnswer409ToTheLoser() throws Exception {
        var account = given().contentType(ContentType.JSON)
                .body("""
                    {
                      "customer": {"full_name":"Bruno","document":"55566677700","email":"bruno@example.com","phone":null,"subject":"bruno-subject-1"},
                      "address": {"street":"Rua B","number":"200","city":"Fortaleza","state":"CE","zip_code":"60000-111","country":"BR"}
                    }
                    """)
                .when().post("/accounts")
                .then().statusCode(201)
                .body("physical_card_id", notNullValue())
                .extract();
        String accountId = account.path("account_id");
        String physicalCardId = account.path("physical_card_id");

        int before = physicalCardsOf(accountId);
        assertEquals(1, before, "a conta nasce com um cartao fisico");

        List<Integer> answers = reissueConcurrently(physicalCardId);

        // A corrida termina de um de dois jeitos legitimos, conforme a ordem em que as transacoes
        // se encaixam: a perdedora encontra o cartao antigo ja inativo (403) ou bate na restricao
        // unica do predecessor (409). O que nao pode acontecer e 5xx — era o que a violacao de
        // restricao nao traduzida produzia.
        assertEquals(200, answers.get(0), "uma das reemissoes conclui");
        assertTrue(
                answers.get(1) == 403 || answers.get(1) == 409,
                "a perdedora recebe conflito estavel, e nao erro de servidor: " + answers);
        assertEquals(before + 1, physicalCardsOf(accountId), "a corrida criou um unico cartao novo");
    }

    /** Dispara as reemissoes ao mesmo tempo e devolve os status ordenados. */
    private static List<Integer> reissueConcurrently(String physicalCardId) throws Exception {
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(REISSUERS);
        try {
            List<Callable<Integer>> calls = new ArrayList<>();
            for (int reissuer = 0; reissuer < REISSUERS; reissuer++) {
                calls.add(() -> {
                    start.await(30, TimeUnit.SECONDS);
                    return given().contentType(ContentType.JSON)
                            .body("{\"reason\":\"THEFT\"}")
                            .when().post("/physical-cards/" + physicalCardId + "/reissue")
                            .then().extract().statusCode();
                });
            }
            List<Future<Integer>> running = new ArrayList<>();
            for (Callable<Integer> call : calls) {
                running.add(pool.submit(call));
            }
            start.countDown();
            List<Integer> answers = new ArrayList<>();
            for (Future<Integer> answer : running) {
                answers.add(answer.get(60, TimeUnit.SECONDS));
            }
            answers.sort(Comparator.naturalOrder());
            return answers;
        }
        finally {
            pool.shutdownNow();
        }
    }

    private static int physicalCardsOf(String accountId) {
        return given().queryParam("accountId", accountId)
                .when().get("/physical-cards")
                .then().statusCode(200)
                .extract().jsonPath().getList("$").size();
    }

}
