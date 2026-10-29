package com.altbank.cardsapi.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.sql.SQLException;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;

import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;

import com.altbank.cardsapi.application.exception.ConflictException;
import com.altbank.cardsapi.application.exception.ErrorCode;

class UniqueConflictsTest {

    private static final String CONSTRAINT = "uk_physical_cards_previous";

    private final EntityManager entityManager = mock(EntityManager.class);

    @Test
    void aCleanFlushPassesThrough() {
        UniqueConflicts.flushTranslating(entityManager, CONSTRAINT, UniqueConflictsTest::conflict);

        verify(entityManager).flush();
    }

    @Test
    void theNamedConstraintBecomesAConflict() {
        doThrow(violation(CONSTRAINT)).when(entityManager).flush();

        ConflictException thrown = assertThrows(
                ConflictException.class,
                () -> UniqueConflicts.flushTranslating(entityManager, CONSTRAINT, UniqueConflictsTest::conflict));

        assertEquals(ErrorCode.PHYSICAL_CARD_ALREADY_REISSUED, thrown.errorCode());
        assertEquals(409, thrown.httpStatus());
    }

    @Test
    void theConstraintNameIsComparedWithoutCase() {
        doThrow(violation(CONSTRAINT.toUpperCase())).when(entityManager).flush();

        assertThrows(
                ConflictException.class,
                () -> UniqueConflicts.flushTranslating(entityManager, CONSTRAINT, UniqueConflictsTest::conflict));
    }

    @Test
    void anotherConstraintKeepsItsOwnFailure() {
        PersistenceException other = violation("uk_customers_document");
        doThrow(other).when(entityManager).flush();

        PersistenceException thrown = assertThrows(
                PersistenceException.class,
                () -> UniqueConflicts.flushTranslating(entityManager, CONSTRAINT, UniqueConflictsTest::conflict));

        assertSame(other, thrown, "falha de outra restricao nao pode virar resposta de negocio");
    }

    @Test
    void anUnrelatedPersistenceFailureKeepsItsOwnFailure() {
        PersistenceException broken = new PersistenceException("connection reset");
        doThrow(broken).when(entityManager).flush();

        assertSame(
                broken,
                assertThrows(
                        PersistenceException.class,
                        () -> UniqueConflicts.flushTranslating(
                                entityManager, CONSTRAINT, UniqueConflictsTest::conflict)));
    }

    /** Violacao como o Hibernate a entrega: embrulhada, com o nome da restricao no meio. */
    private static PersistenceException violation(String constraint) {
        return new PersistenceException(
                "could not execute statement",
                new ConstraintViolationException(
                        "Duplicate entry for key '" + constraint + "'",
                        new SQLException("duplicate", "23000", 1062),
                        constraint));
    }

    private static ConflictException conflict() {
        return new ConflictException(
                ErrorCode.PHYSICAL_CARD_ALREADY_REISSUED,
                "Physical card has already been reissued");
    }

}
