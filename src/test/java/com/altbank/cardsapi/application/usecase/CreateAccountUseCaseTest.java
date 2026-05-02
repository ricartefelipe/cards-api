package com.altbank.cardsapi.application.usecase;

import com.altbank.cardsapi.application.dto.CreateAccountRequest;
import com.altbank.cardsapi.application.dto.CreateAccountResponse;
import com.altbank.cardsapi.application.exception.ConflictException;
import com.altbank.cardsapi.application.exception.ErrorCode;
import com.altbank.cardsapi.application.port.AccountRepository;
import com.altbank.cardsapi.application.port.CarrierPort;
import com.altbank.cardsapi.application.port.CustomerRepository;
import com.altbank.cardsapi.application.port.PhysicalCardRepository;
import com.altbank.cardsapi.domain.model.Customer;
import com.altbank.cardsapi.domain.model.DeliveryStatus;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.altbank.cardsapi.CardsTestFixtures.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateAccountUseCaseTest {

    @Mock
    CustomerRepository customerRepository;
    @Mock
    AccountRepository accountRepository;
    @Mock
    PhysicalCardRepository physicalCardRepository;
    @Mock
    CarrierPort carrierPort;

    @Test
    void create_whenDocumentExists_shouldFail() {
        CreateAccountRequest req = validCreateAccountRequest();
        Customer existing = activeAccount().customer();
        when(customerRepository.findByDocument("99887766554")).thenReturn(Optional.of(existing));

        CreateAccountUseCase useCase = new CreateAccountUseCase(
                customerRepository, accountRepository, physicalCardRepository, carrierPort, CLOCK);

        ConflictException ex = Assertions.assertThrows(ConflictException.class, () -> useCase.create(req));
        Assertions.assertEquals(ErrorCode.CUSTOMER_ALREADY_EXISTS, ex.errorCode());

        verify(accountRepository, never()).persist(any());
        verify(physicalCardRepository, never()).persist(any());
    }

    @Test
    void create_shouldPersistEntitiesAndShipment() {
        CreateAccountRequest req = validCreateAccountRequest();
        when(customerRepository.findByDocument("99887766554")).thenReturn(Optional.empty());
        when(carrierPort.createShipment(any()))
                .thenReturn(new CarrierPort.CarrierShipment("trk_new", DeliveryStatus.SHIPPED));

        CreateAccountUseCase useCase = new CreateAccountUseCase(
                customerRepository, accountRepository, physicalCardRepository, carrierPort, CLOCK);

        CreateAccountResponse response = useCase.create(req);

        Assertions.assertNotNull(response.accountId());
        Assertions.assertEquals("trk_new", response.trackingId());

        verify(customerRepository).persist(any(Customer.class));
        verify(accountRepository).persist(any());
        verify(physicalCardRepository).persist(any());

        ArgumentCaptor<String> addressCaptor = ArgumentCaptor.forClass(String.class);
        verify(carrierPort).createShipment(addressCaptor.capture());
        Assertions.assertTrue(addressCaptor.getValue().contains("Av B"));
    }
}
