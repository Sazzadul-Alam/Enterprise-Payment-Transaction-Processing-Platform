package com.enterprise.payment.common.constant;

public final class KafkaTopicConstants {

    private KafkaTopicConstants() {}

    public static final String PAYMENT_EVENTS = "payment.events";
    public static final String PAYMENT_EVENTS_DLQ = "payment.events.dlq";

    public static final String LEDGER_EVENTS = "ledger.events";
    public static final String LEDGER_EVENTS_DLQ = "ledger.events.dlq";

    public static final String NOTIFICATION_EVENTS = "notification.events";
    public static final String NOTIFICATION_EVENTS_DLQ = "notification.events.dlq";

    public static final String AUDIT_EVENTS = "audit.events";
    public static final String AUDIT_EVENTS_DLQ = "audit.events.dlq";
}
