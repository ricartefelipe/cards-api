package com.altbank.cardsapi.application.audit;

/**
 * Acoes administrativas registradas na trilha de auditoria.
 */
public enum AuditAction {
    CREATE_ACCOUNT,
    CANCEL_ACCOUNT,
    ISSUE_VIRTUAL_CARD,
    SHIPMENT_REQUESTED,
    SHIPMENT_CONFIRMED,
    SHIPMENT_REJECTED,
    SHIPMENT_UNCERTAIN
}
