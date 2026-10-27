package com.altbank.cardsapi.application.security;

import com.altbank.cardsapi.application.port.AccountRepository;
import com.altbank.cardsapi.domain.model.Account;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.altbank.cardsapi.CardsTestFixtures.accountOwnedBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OwnershipPolicyTest {

    @Mock
    CallerContext caller;

    @Mock
    AccountRepository accountRepository;

    private OwnershipPolicy policy() {
        return new OwnershipPolicy(caller, accountRepository);
    }

    @Test
    void admin_canAccessAnyAccount() {
        when(caller.isAdmin()).thenReturn(true);

        Assertions.assertTrue(policy().canAccess(accountOwnedBy("someone-else")));
    }

    @Test
    void owner_canAccessWhenSubjectMatches() {
        when(caller.isAdmin()).thenReturn(false);
        when(caller.subject()).thenReturn("owner-a");

        Assertions.assertTrue(policy().canAccess(accountOwnedBy("owner-a")));
    }

    @Test
    void user_cannotAccessAnotherOwnersAccount() {
        when(caller.isAdmin()).thenReturn(false);
        when(caller.subject()).thenReturn("owner-a");

        Assertions.assertFalse(policy().canAccess(accountOwnedBy("owner-b")));
    }

    @Test
    void user_cannotAccessAccountWithoutLinkedSubject() {
        when(caller.isAdmin()).thenReturn(false);
        when(caller.subject()).thenReturn("owner-a");

        Assertions.assertFalse(policy().canAccess(accountOwnedBy(null)));
    }

    @Test
    void callerAccountId_isEmptyForAdmin() {
        when(caller.isAdmin()).thenReturn(true);

        Assertions.assertTrue(policy().callerAccountId().isEmpty());
        verifyNoInteractions(accountRepository);
    }

    @Test
    void callerAccountId_resolvesOwnedAccountForUser() {
        Account owned = accountOwnedBy("owner-a");
        when(caller.isAdmin()).thenReturn(false);
        when(caller.subject()).thenReturn("owner-a");
        when(accountRepository.findBySubject("owner-a")).thenReturn(Optional.of(owned));

        Optional<UUID> result = policy().callerAccountId();

        Assertions.assertEquals(Optional.of(owned.id()), result);
    }

    @Test
    void callerAccountId_isEmptyWhenSubjectMissing() {
        when(caller.isAdmin()).thenReturn(false);
        lenient().when(caller.subject()).thenReturn(null);

        Assertions.assertTrue(policy().callerAccountId().isEmpty());
        verifyNoInteractions(accountRepository);
    }
}
