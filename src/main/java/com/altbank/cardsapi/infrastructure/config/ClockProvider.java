package com.altbank.cardsapi.infrastructure.config;

import jakarta.inject.Inject;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import java.time.Clock;

@ApplicationScoped
public class ClockProvider {

    @Produces
    public Clock clock() {
        return Clock.systemUTC();
    }
}
