package com.altbank.cardsapi.infrastructure.config;

import com.altbank.cardsapi.application.port.TransactionRunner;
import io.quarkus.narayana.jta.QuarkusTransaction;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.function.Supplier;

@ApplicationScoped
public class QuarkusTransactionRunner implements TransactionRunner {

    @Override
    public <T> T inTransaction(Supplier<T> work) {
        return QuarkusTransaction.requiringNew().call(work::get);
    }
}
