package com.altbank.cardsapi.application.audit;

import com.altbank.cardsapi.application.port.AuditRepository;
import com.altbank.cardsapi.application.security.CallerContext;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

    @Mock
    AuditRepository auditRepository;

    @Mock
    CallerContext caller;

    @Test
    void record_persistsActionWithActorAndTarget() {
        when(caller.subject()).thenReturn("admin-sub");
        UUID targetId = UUID.randomUUID();

        new AuditService(auditRepository, caller).record(AuditAction.CANCEL_ACCOUNT, "account", targetId);

        verify(auditRepository).record(
                argThat(corr -> corr != null && !corr.isBlank()),
                eq("admin-sub"),
                eq("CANCEL_ACCOUNT"),
                eq("account"),
                eq(targetId.toString()));
    }

    @Test
    void record_toleratesNullTarget() {
        when(caller.subject()).thenReturn("admin-sub");

        new AuditService(auditRepository, caller).record(AuditAction.CREATE_ACCOUNT, "account", null);

        verify(auditRepository).record(
                argThat(corr -> corr != null && !corr.isBlank()),
                eq("admin-sub"),
                eq("CREATE_ACCOUNT"),
                eq("account"),
                isNull());
    }
}
