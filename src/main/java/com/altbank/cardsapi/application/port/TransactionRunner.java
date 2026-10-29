package com.altbank.cardsapi.application.port;

import java.util.function.Supplier;

/** Executa um trecho em transacao propria, para que o efeito externo fique fora dela. */
public interface TransactionRunner {

    <T> T inTransaction(Supplier<T> work);

    default void inTransaction(Runnable work) {
        inTransaction(() -> {
            work.run();
            return null;
        });
    }
}
