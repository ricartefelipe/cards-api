package com.altbank.cardsapi.application.security;

import com.altbank.cardsapi.application.port.AccountRepository;
import com.altbank.cardsapi.domain.model.Account;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Regra de autorizacao por objeto. O papel {@code admin}/suporte tem acesso pleno;
 * o titular ({@code user}) so acessa recursos da propria conta, vinculada pelo
 * {@code subject} do IdP. Possuir o papel {@code user} nao equivale a posse do recurso.
 */
@ApplicationScoped
public class OwnershipPolicy {

    private final CallerContext caller;
    private final AccountRepository accountRepository;

    @Inject
    public OwnershipPolicy(CallerContext caller, AccountRepository accountRepository) {
        this.caller = Objects.requireNonNull(caller, "caller");
        this.accountRepository = Objects.requireNonNull(accountRepository, "accountRepository");
    }

    public boolean isAdmin() {
        return caller.isAdmin();
    }

    /**
     * {@code true} se o chamador pode acessar recursos da conta informada.
     */
    public boolean canAccess(Account account) {
        Objects.requireNonNull(account, "account");
        if (caller.isAdmin()) {
            return true;
        }
        String ownerSubject = account.customer().subject();
        String callerSubject = caller.subject();
        return ownerSubject != null && ownerSubject.equals(callerSubject);
    }

    /**
     * Conta do titular chamador (nao-admin), quando houver vinculo por subject.
     * Vazio para admin (que nao e restrito a uma unica conta) ou titular sem conta vinculada.
     */
    public Optional<UUID> callerAccountId() {
        if (caller.isAdmin()) {
            return Optional.empty();
        }
        String subject = caller.subject();
        if (subject == null || subject.isBlank()) {
            return Optional.empty();
        }
        return accountRepository.findBySubject(subject).map(Account::id);
    }
}
