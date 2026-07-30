package com.altbank.cardsapi.application.dto;

import com.altbank.cardsapi.domain.model.Account;
import com.altbank.cardsapi.domain.model.Address;
import com.altbank.cardsapi.domain.model.Customer;
import com.altbank.cardsapi.domain.model.PhysicalCard;
import com.altbank.cardsapi.domain.model.VirtualCard;
import java.util.List;
import java.util.UUID;

public final class QueryResponses {

    private QueryResponses() {
    }

    public static AccountSummaryResponse toAccountSummary(Account account) {
        Customer customer = account.customer();
        return new AccountSummaryResponse(
                account.id(),
                account.status().name(),
                customer.id(),
                customer.fullName(),
                customer.document(),
                account.createdAt(),
                account.cancelledAt()
        );
    }

    public static AccountDetailResponse toAccountDetail(Account account,
                                                        List<UUID> physicalCardIds,
                                                        List<UUID> virtualCardIds) {
        return new AccountDetailResponse(
                account.id(),
                account.status().name(),
                account.createdAt(),
                account.cancelledAt(),
                toCustomerSummary(account.customer()),
                physicalCardIds,
                virtualCardIds
        );
    }

    public static CustomerSummaryResponse toCustomerSummary(Customer customer) {
        return new CustomerSummaryResponse(
                customer.id(),
                customer.fullName(),
                customer.document(),
                customer.email(),
                customer.phone(),
                customer.createdAt()
        );
    }

    public static CustomerDetailResponse toCustomerDetail(Customer customer, UUID accountId) {
        return new CustomerDetailResponse(
                customer.id(),
                customer.fullName(),
                customer.document(),
                customer.email(),
                customer.phone(),
                toAddress(customer.address()),
                accountId,
                customer.createdAt()
        );
    }

    public static AddressResponse toAddress(Address address) {
        return new AddressResponse(
                address.street(),
                address.number(),
                address.city(),
                address.state(),
                address.zipCode(),
                address.country()
        );
    }

    public static PhysicalCardResponse toPhysicalCard(PhysicalCard card) {
        return new PhysicalCardResponse(
                card.id(),
                card.account().id(),
                card.status().name(),
                card.trackingId(),
                card.deliveryStatus().name(),
                card.deliveryDate(),
                card.deliveryReturnReason(),
                card.deliveryAddress(),
                card.deliveredAt(),
                card.validatedAt(),
                card.reissueReason() == null ? null : card.reissueReason().name(),
                card.previousPhysicalCardId(),
                card.createdAt(),
                card.deactivatedAt()
        );
    }

    public static VirtualCardResponse toVirtualCard(VirtualCard card) {
        return new VirtualCardResponse(
                card.id(),
                card.account().id(),
                card.status().name(),
                card.processorAccountId(),
                card.processorCardId(),
                card.cvvExpirationAt(),
                card.reissueReason() == null ? null : card.reissueReason().name(),
                card.previousVirtualCardId(),
                card.createdAt(),
                card.deactivatedAt()
        );
    }
}
