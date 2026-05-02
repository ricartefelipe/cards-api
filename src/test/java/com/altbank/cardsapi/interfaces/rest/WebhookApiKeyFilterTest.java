package com.altbank.cardsapi.interfaces.rest;

import com.altbank.cardsapi.application.exception.ErrorCode;
import com.altbank.cardsapi.application.exception.UnauthorizedException;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.UriInfo;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class WebhookApiKeyFilterTest {

    @Mock
    ContainerRequestContext requestContext;

    @Mock
    UriInfo uriInfo;

    @Test
    void filter_carrierPath_badKey_shouldThrow() throws Exception {
        WebhookApiKeyFilter filter = new WebhookApiKeyFilter("c-secret", "p-secret");

        Mockito.when(requestContext.getUriInfo()).thenReturn(uriInfo);
        Mockito.when(uriInfo.getPath()).thenReturn("webhooks/carrier/delivery");
        Mockito.when(requestContext.getHeaderString(WebhookApiKeyFilter.HEADER_NAME)).thenReturn("wrong");

        UnauthorizedException ex = Assertions.assertThrows(UnauthorizedException.class, () -> filter.filter(requestContext));
        Assertions.assertEquals(ErrorCode.WEBHOOK_UNAUTHORIZED, ex.errorCode());
    }

    @Test
    void filter_carrierPath_correctKey_shouldPass() throws Exception {
        WebhookApiKeyFilter filter = new WebhookApiKeyFilter("c-secret", "p-secret");

        Mockito.when(requestContext.getUriInfo()).thenReturn(uriInfo);
        Mockito.when(uriInfo.getPath()).thenReturn("webhooks/carrier/delivery");
        Mockito.when(requestContext.getHeaderString(WebhookApiKeyFilter.HEADER_NAME)).thenReturn("c-secret");

        filter.filter(requestContext);
    }

    @Test
    void filter_accountsPath_shouldNotReadHeader() throws Exception {
        WebhookApiKeyFilter filter = new WebhookApiKeyFilter("c-secret", "p-secret");

        Mockito.when(requestContext.getUriInfo()).thenReturn(uriInfo);
        Mockito.when(uriInfo.getPath()).thenReturn("accounts");

        filter.filter(requestContext);

        verify(requestContext, never()).getHeaderString(any());
    }
}
