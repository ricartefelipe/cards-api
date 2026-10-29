package com.altbank.cardsapi.infrastructure.persistence;

import java.util.Objects;
import java.util.function.Supplier;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;

import org.hibernate.exception.ConstraintViolationException;

import com.altbank.cardsapi.application.exception.AppException;

/**
 * Traduz a violacao de uma restricao unica conhecida em conflito de aplicacao.
 *
 * <p>O flush e antecipado para que a corrida apareca aqui, e nao no commit da transacao, longe
 * de quem sabe o que ela significa. Somente a restricao nomeada e traduzida: qualquer outra
 * falha de persistencia continua subindo como esta, para que um defeito nao se disfarce de
 * resposta de negocio.
 */
final class UniqueConflicts {

    private UniqueConflicts() {
    }

    static void flushTranslating(EntityManager entityManager, String constraint, Supplier<AppException> conflict) {
        Objects.requireNonNull(entityManager, "entityManager");
        Objects.requireNonNull(constraint, "constraint");
        Objects.requireNonNull(conflict, "conflict");
        try {
            entityManager.flush();
        }
        catch (PersistenceException failure) {
            if (violates(failure, constraint)) {
                throw conflict.get();
            }
            throw failure;
        }
    }

    private static boolean violates(Throwable failure, String constraint) {
        for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
            if (names(cause, constraint)) {
                return true;
            }
        }
        return false;
    }

    private static boolean names(Throwable cause, String constraint) {
        if (cause instanceof ConstraintViolationException violation
                && constraint.equalsIgnoreCase(violation.getConstraintName())) {
            return true;
        }
        // O MariaDB nem sempre devolve o nome da restricao em campo proprio; quando nao devolve,
        // ele aparece no texto do erro. A comparacao segue sendo com esta restricao, e so com ela.
        String message = cause.getMessage();
        return message != null && message.toLowerCase().contains(constraint.toLowerCase());
    }

}
