package com.altbank.cardsapi.infrastructure.security;

import com.altbank.cardsapi.application.security.CallerContext;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import java.security.Principal;
import java.util.Objects;
import org.eclipse.microprofile.jwt.JsonWebToken;

/**
 * {@link CallerContext} apoiado na identidade verificada da requisicao.
 *
 * <p>O subject vem do claim {@code sub} do token que autenticou a chamada, lido do principal da
 * identidade: com OIDC bearer o principal <em>e</em> o {@link JsonWebToken} da requisicao. Sem
 * token nao existe subject, e nenhuma consulta por dono encontra registro. O papel admin vem das
 * roles da identidade.
 */
@RequestScoped
public class JwtCallerContext implements CallerContext {

    static final String ADMIN_ROLE = "admin";

    private final SecurityIdentity identity;

    @Inject
    public JwtCallerContext(SecurityIdentity identity) {
        this.identity = Objects.requireNonNull(identity, "identity");
    }

    @Override
    public String subject() {
        Principal principal = identity.getPrincipal();
        if (principal instanceof JsonWebToken token) {
            return token.getSubject();
        }
        return null;
    }

    @Override
    public boolean isAdmin() {
        return identity.hasRole(ADMIN_ROLE);
    }
}
