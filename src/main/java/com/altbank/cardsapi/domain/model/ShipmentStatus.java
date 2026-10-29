package com.altbank.cardsapi.domain.model;

/**
 * Situacao de um pedido de remessa junto a transportadora.
 *
 * <p>UNCERTAIN e o estado de quem chamou e nao soube a resposta (timeout, queda antes de
 * finalizar): a remessa pode ter sido aceita, e por isso nao e reenviada automaticamente —
 * consulta ou cancelamento dependem de contrato com o parceiro.
 */
public enum ShipmentStatus {
    PENDING,
    IN_PROGRESS,
    CONFIRMED,
    REJECTED,
    UNCERTAIN
}
