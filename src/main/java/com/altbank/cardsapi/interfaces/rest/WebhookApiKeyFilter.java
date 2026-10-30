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
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Objects;

@Provider
@Priority(Priorities.AUTHENTICATION)
public class WebhookApiKeyFilter implements ContainerRequestFilter {

    public static final String HEADER_NAME = "X-Webhook-Api-Key";
    private static final String CARRIER_PATH = "webhooks/carrier";
    private static final String PROCESSOR_PATH = "webhooks/processor";

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
        String relativePath = withoutLeadingSlash(path);
        if (relativePath.startsWith(CARRIER_PATH)) {
            validateKey(requestContext, carrierApiKey);
        } else if (relativePath.startsWith(PROCESSOR_PATH)) {
            validateKey(requestContext, processorApiKey);
        }
    }

    // O Quarkus REST entrega o caminho com barra inicial ("/webhooks/..."); sem normalizar, nenhum
    // webhook era reconhecido e a chave nunca era exigida.
    private static String withoutLeadingSlash(String path) {
        return path.startsWith("/") ? path.substring(1) : path;
    }

    private void validateKey(ContainerRequestContext requestContext, String expected) {
        String provided = requestContext.getHeaderString(HEADER_NAME);
        if (provided == null || !timingSafeEquals(provided, expected)) {
            throw new UnauthorizedException(ErrorCode.WEBHOOK_UNAUTHORIZED, "Invalid webhook API key");
        }
    }

    private static boolean timingSafeEquals(String a, String b) {
        byte[] aBytes = a.getBytes(StandardCharsets.UTF_8);
        byte[] bBytes = b.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(aBytes, bBytes);
    }
}
