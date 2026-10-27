package com.altbank.cardsapi.application;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Fingerprint estavel de um evento de webhook, para deduplicacao idempotente.
 * E um hash (SHA-256) dos campos identificadores — nao armazena material sensivel
 * em claro e nao inclui o CVV.
 */
public final class WebhookEventFingerprint {

    private static final String SEPARATOR = "|";

    private WebhookEventFingerprint() {
    }

    public static String of(String eventType, String... parts) {
        StringBuilder canonical = new StringBuilder(eventType == null ? "" : eventType);
        for (String part : parts) {
            canonical.append(SEPARATOR).append(part == null ? "" : part);
        }
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(canonical.toString().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponivel", e);
        }
    }
}
