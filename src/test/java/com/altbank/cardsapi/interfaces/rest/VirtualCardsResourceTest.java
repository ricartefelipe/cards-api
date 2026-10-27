package com.altbank.cardsapi.interfaces.rest;

import com.altbank.cardsapi.application.dto.CvvResponse;
import com.altbank.cardsapi.application.usecase.GetVirtualCardCvvUseCase;
import com.altbank.cardsapi.application.usecase.QueryVirtualCardsUseCase;
import com.altbank.cardsapi.application.usecase.ReissueVirtualCardUseCase;
import java.time.LocalDateTime;
import java.util.UUID;
import org.jboss.resteasy.reactive.RestResponse;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VirtualCardsResourceTest {

    @Mock
    GetVirtualCardCvvUseCase getVirtualCardCvvUseCase;

    @Mock
    ReissueVirtualCardUseCase reissueVirtualCardUseCase;

    @Mock
    QueryVirtualCardsUseCase queryVirtualCardsUseCase;

    @Test
    void getCvv_setsNoStoreCacheHeaders() {
        UUID cardId = UUID.randomUUID();
        when(getVirtualCardCvvUseCase.get(cardId))
                .thenReturn(new CvvResponse(123, LocalDateTime.of(2026, 1, 31, 13, 0, 0)));

        VirtualCardsResource resource = new VirtualCardsResource(
                getVirtualCardCvvUseCase, reissueVirtualCardUseCase, queryVirtualCardsUseCase);

        RestResponse<CvvResponse> response = resource.getCvv(cardId);

        Assertions.assertEquals("no-store", response.getHeaders().getFirst("Cache-Control"));
        Assertions.assertEquals("no-cache", response.getHeaders().getFirst("Pragma"));
        Assertions.assertEquals(123, response.getEntity().cvv());
    }
}
