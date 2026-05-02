package com.altbank.cardsapi;

import com.altbank.cardsapi.application.dto.CreateAccountRequest;
import com.altbank.cardsapi.domain.model.Account;
import com.altbank.cardsapi.domain.model.Address;
import com.altbank.cardsapi.domain.model.Customer;
import com.altbank.cardsapi.domain.model.DeliveryStatus;
import com.altbank.cardsapi.domain.model.PhysicalCard;
import com.altbank.cardsapi.domain.model.VirtualCard;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

public final class CardsTestFixtures {

    public static final Clock CLOCK = Clock.fixed(Instant.parse("2026-01-31T12:00:00Z"), ZoneOffset.UTC);

    private CardsTestFixtures() {
    }

    public static Account activeAccount() {
        Address address = new Address("Rua A", "100", "Fortaleza", "CE", "60000-000", "BR");
        Customer customer = new Customer("Fulano", "12345678900", "fulano@example.com", null, address, CLOCK);
        return new Account(customer, CLOCK);
    }

    public static PhysicalCard activePhysicalUndelivered(Account account) {
        return new PhysicalCard(
                account,
                "trk_1",
                DeliveryStatus.SHIPPED,
                account.customer().address().asSingleLine(),
                null,
                null,
                CLOCK);
    }

    public static PhysicalCard activePhysicalDeliveredValidated(Account account) {
        PhysicalCard card = activePhysicalUndelivered(account);
        card.applyCarrierDeliveryUpdate(
                DeliveryStatus.DELIVERED,
                LocalDateTime.of(2026, 1, 31, 12, 0, 0),
                null,
                account.customer().address().asSingleLine(),
                CLOCK);
        card.validate(CLOCK);
        return card;
    }

    public static PhysicalCard activePhysicalDeliveredNotValidated(Account account) {
        PhysicalCard card = activePhysicalUndelivered(account);
        card.applyCarrierDeliveryUpdate(
                DeliveryStatus.DELIVERED,
                LocalDateTime.of(2026, 1, 31, 12, 0, 0),
                null,
                account.customer().address().asSingleLine(),
                CLOCK);
        return card;
    }

    public static VirtualCard activeVirtual(Account account, String processorCardId) {
        return new VirtualCard(
                account,
                account.id().toString(),
                processorCardId,
                LocalDateTime.of(2026, 1, 31, 12, 30, 0),
                CLOCK);
    }

    public static CreateAccountRequest validCreateAccountRequest() {
        return new CreateAccountRequest(
                new CreateAccountRequest.CustomerInput("Fulano Novo", "99887766554", "novo@example.com", null),
                new CreateAccountRequest.AddressInput("Av B", "200", "São Paulo", "SP", "01000-000", "BR"));
    }
}
