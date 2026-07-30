package com.altbank.cardsapi.application.usecase;

import com.altbank.cardsapi.application.dto.AccountDetailResponse;
import com.altbank.cardsapi.application.dto.AccountSummaryResponse;
import com.altbank.cardsapi.application.dto.QueryResponses;
import com.altbank.cardsapi.application.exception.ErrorCode;
import com.altbank.cardsapi.application.exception.NotFoundException;
import com.altbank.cardsapi.application.port.AccountRepository;
import com.altbank.cardsapi.application.port.PhysicalCardRepository;
import com.altbank.cardsapi.application.port.VirtualCardRepository;
import com.altbank.cardsapi.domain.model.Account;
import com.altbank.cardsapi.domain.model.PhysicalCard;
import com.altbank.cardsapi.domain.model.VirtualCard;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@ApplicationScoped
public class QueryAccountsUseCase {

    private final AccountRepository accountRepository;
    private final PhysicalCardRepository physicalCardRepository;
    private final VirtualCardRepository virtualCardRepository;

    @Inject
    public QueryAccountsUseCase(AccountRepository accountRepository,
                                PhysicalCardRepository physicalCardRepository,
                                VirtualCardRepository virtualCardRepository) {
        this.accountRepository = Objects.requireNonNull(accountRepository, "accountRepository");
        this.physicalCardRepository = Objects.requireNonNull(physicalCardRepository, "physicalCardRepository");
        this.virtualCardRepository = Objects.requireNonNull(virtualCardRepository, "virtualCardRepository");
    }

    @Transactional
    public List<AccountSummaryResponse> list() {
        return accountRepository.listAll().stream().map(QueryResponses::toAccountSummary).toList();
    }

    @Transactional
    public AccountDetailResponse get(UUID accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.ACCOUNT_NOT_FOUND, "Account not found"));
        List<UUID> physicalIds = physicalCardRepository.listByAccountId(accountId).stream()
                .map(PhysicalCard::id)
                .toList();
        List<UUID> virtualIds = virtualCardRepository.listByAccountId(accountId).stream()
                .map(VirtualCard::id)
                .toList();
        return QueryResponses.toAccountDetail(account, physicalIds, virtualIds);
    }
}
