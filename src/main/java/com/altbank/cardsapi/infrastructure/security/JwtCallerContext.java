package com.altbank.cardsapi.infrastructure.security;

import com.altbank.cardsapi.application.security.CallerContext;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import java.util.Objects;
import org.eclipse.microprofile.jwt.JsonWebToken;

/**
 * {@link CallerContext} apoiado no token OIDC/JWT verificado da requisicao.
 * O subject vem do claim {@code sub}; o papel admin vem das roles do token.
 */
@RequestScoped
public class JwtCallerContext implements CallerContext {

    static final String ADMIN_ROLE = "admin";

    private final SecurityIdentity identity;
    private final JsonWebToken jwt;

    @Inject
    public JwtCallerContext(SecurityIdentity identity, JsonWebToken jwt) {
        this.identity = Objects.requireNonNull(identity, "identity");
        this.jwt = Objects.requireNonNull(jwt, "jwt");
    }

    @Override
    public String subject() {
        return jwt.getSubject();
    }

    @Override
    public boolean isAdmin() {
        return identity.hasRole(ADMIN_ROLE);
    }
}
