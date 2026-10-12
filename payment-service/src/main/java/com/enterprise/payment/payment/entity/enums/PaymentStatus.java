package com.enterprise.payment.payment.entity.enums;

/**
 * Payment lifecycle state machine.
 * Valid transitions:
 *   PENDING → AUTHORIZED → CAPTURED
 *   PENDING → FAILED
 *   AUTHORIZED → FAILED
 *   CAPTURED → REFUNDED
 */
public enum PaymentStatus {
    PENDING,
    AUTHORIZED,
    CAPTURED,
    FAILED,
    REFUNDED
}
