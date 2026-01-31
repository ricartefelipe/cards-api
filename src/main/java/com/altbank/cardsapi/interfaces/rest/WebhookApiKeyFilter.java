package com.altbank.cardsapi.interfaces.rest;

import jakarta.inject.Inject;
import com.altbank.cardsapi.application.exception.ErrorCode;
import com.altbank.cardsapi.application.exception.UnauthorizedException;
import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.ext.Provider;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import java.io.IOException;
import java.util.Objects;

@Provider
@Priority(Priorities.AUTHENTICATION)
public class WebhookApiKeyFilter implements ContainerRequestFilter {

    public static final String HEADER_NAME = "X-Webhook-Api-Key";

    private final String carrierApiKey;
    private final String processorApiKey;

    @Inject

    public WebhookApiKeyFilter(@ConfigProperty(name = "cards.webhooks.carrier.api-key") String carrierApiKey,
                               @ConfigProperty(name = "cards.webhooks.processor.api-key") String processorApiKey) {
        this.carrierApiKey = Objects.requireNonNull(carrierApiKey, "carrierApiKey");
        this.processorApiKey = Objects.requireNonNull(processorApiKey, "processorApiKey");
    }

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        String path = requestContext.getUriInfo().getPath();
        if (path == null) {
            return;
        }
        if (path.startsWith("webhooks/carrier")) {
            validateKey(requestContext, carrierApiKey);
        } else if (path.startsWith("webhooks/processor")) {
            validateKey(requestContext, processorApiKey);
        }
    }

    private void validateKey(ContainerRequestContext requestContext, String expected) {
        String provided = requestContext.getHeaderString(HEADER_NAME);
        if (provided == null || !provided.equals(expected)) {
            throw new UnauthorizedException(ErrorCode.WEBHOOK_UNAUTHORIZED, "Invalid webhook API key");
        }
    }
}
