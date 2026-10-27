package com.altbank.cardsapi.application.security;

/**
 * Identidade do chamador autenticado, derivada do token verificado.
 * Abstrai o transporte (JWT/OIDC) para a camada de aplicacao.
 */
public interface CallerContext {

    /**
     * Subject (claim {@code sub}) do chamador, ou {@code null} se nao autenticado.
     */
    String subject();

    /**
     * {@code true} se o chamador possui o papel administrativo/suporte.
     */
    boolean isAdmin();
}
