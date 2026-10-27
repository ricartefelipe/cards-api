package com.altbank.cardsapi.infrastructure.external;

import com.altbank.cardsapi.application.port.ProcessorPort;
import io.quarkus.arc.profile.IfBuildProfile;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.LocalDateTime;

/**
 * Adaptador fail-closed usado em producao enquanto nao houver processadora real contratada.
 * Satisfaz a injecao de {@link ProcessorPort}, mas qualquer operacao real falha de forma
 * explicita. A inicializacao de producao ja e bloqueada antes (ProductionReadinessGuard);
 * esta classe e a segunda barreira, caso o gate seja afrouxado por configuracao.
 */
@ApplicationScoped
@IfBuildProfile("prod")
public class UnconfiguredProcessorPort implements ProcessorPort {

    private static final String MESSAGE =
            "Processadora nao configurada: nenhum adaptador real contratado esta ativo em producao.";

    @Override
    public IssuedVirtualCard issueVirtualCard(String processorAccountId) {
        throw new IllegalStateException(MESSAGE);
    }

    @Override
    public CvvData getCurrentCvv(String processorCardId) {
        throw new IllegalStateException(MESSAGE);
    }

    @Override
    public void acceptCvvRotationWebhook(String processorAccountId, String processorCardId, int nextCvv, LocalDateTime expirationDate) {
        throw new IllegalStateException(MESSAGE);
    }

    @Override
    public void deactivateCard(String processorCardId) {
        throw new IllegalStateException(MESSAGE);
    }
}
