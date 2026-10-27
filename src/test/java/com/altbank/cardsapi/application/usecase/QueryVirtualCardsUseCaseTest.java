package com.altbank.cardsapi.application.usecase;

import com.altbank.cardsapi.application.dto.VirtualCardResponse;
import com.altbank.cardsapi.application.exception.ErrorCode;
import com.altbank.cardsapi.application.exception.NotFoundException;
import com.altbank.cardsapi.application.port.VirtualCardRepository;
import com.altbank.cardsapi.application.security.OwnershipPolicy;
import com.altbank.cardsapi.domain.model.Account;
import com.altbank.cardsapi.domain.model.VirtualCard;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.altbank.cardsapi.CardsTestFixtures.accountOwnedBy;
import static com.altbank.cardsapi.CardsTestFixtures.activeVirtual;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QueryVirtualCardsUseCaseTest {

    @Mock
    VirtualCardRepository virtualCardRepository;

    @Mock
    OwnershipPolicy ownershipPolicy;

    private QueryVirtualCardsUseCase useCase() {
        return new QueryVirtualCardsUseCase(virtualCardRepository, ownershipPolicy);
    }

    @Test
    void list_whenAdminWithoutAccount_returnsAll() {
        Account account = accountOwnedBy("owner-a");
        when(ownershipPolicy.isAdmin()).thenReturn(true);
        when(virtualCardRepository.listAll(org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.anyInt()))
                .thenReturn(List.of(activeVirtual(account, "pc_a")));

        List<VirtualCardResponse> result = useCase().list(null, null, null);

        Assertions.assertEquals(1, result.size());
    }

    @Test
    void list_whenOwner_isScopedToOwnAccount() {
        Account account = accountOwnedBy("owner-a");
        UUID ownAccountId = account.id();
        when(ownershipPolicy.isAdmin()).thenReturn(false);
        when(ownershipPolicy.callerAccountId()).thenReturn(Optional.of(ownAccountId));
        when(virtualCardRepository.listByAccountId(ownAccountId))
                .thenReturn(List.of(activeVirtual(account, "pc_a")));

        List<VirtualCardResponse> result = useCase().list(null, null, null);

        Assertions.assertEquals(1, result.size());
    }

    @Test
    void list_whenOwnerRequestsAnotherAccountId_returnsEmptyWithoutQuery() {
        when(ownershipPolicy.isAdmin()).thenReturn(false);
        when(ownershipPolicy.callerAccountId()).thenReturn(Optional.of(UUID.randomUUID()));

        List<VirtualCardResponse> result = useCase().list(UUID.randomUUID(), null, null);

        Assertions.assertTrue(result.isEmpty());
        verify(virtualCardRepository, never()).listByAccountId(org.mockito.ArgumentMatchers.any());
        verify(virtualCardRepository, never())
                .listAll(org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.anyInt());
    }

    @Test
    void list_whenOwnerHasNoLinkedAccount_returnsEmpty() {
        when(ownershipPolicy.isAdmin()).thenReturn(false);
        when(ownershipPolicy.callerAccountId()).thenReturn(Optional.empty());

        Assertions.assertTrue(useCase().list(null, null, null).isEmpty());
        verify(virtualCardRepository, never()).listByAccountId(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void get_whenCallerIsNotOwner_failsAsNotFound() {
        Account account = accountOwnedBy("owner-b");
        VirtualCard card = activeVirtual(account, "pc_b");
        when(virtualCardRepository.findById(card.id())).thenReturn(Optional.of(card));
        when(ownershipPolicy.canAccess(account)).thenReturn(false);

        NotFoundException ex = Assertions.assertThrows(NotFoundException.class, () -> useCase().get(card.id()));
        Assertions.assertEquals(ErrorCode.VIRTUAL_CARD_NOT_FOUND, ex.errorCode());
    }

    @Test
    void get_whenOwner_returnsCard() {
        Account account = accountOwnedBy("owner-a");
        VirtualCard card = activeVirtual(account, "pc_a");
        when(virtualCardRepository.findById(card.id())).thenReturn(Optional.of(card));
        when(ownershipPolicy.canAccess(account)).thenReturn(true);

        Assertions.assertNotNull(useCase().get(card.id()));
    }
}
