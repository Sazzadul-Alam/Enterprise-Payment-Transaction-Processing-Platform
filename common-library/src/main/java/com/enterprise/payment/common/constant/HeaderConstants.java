package com.enterprise.payment.common.constant;

public final class HeaderConstants {

    private HeaderConstants() {}

    public static final String CORRELATION_ID = "X-Correlation-Id";
    public static final String IDEMPOTENCY_KEY = "Idempotency-Key";
    public static final String USER_ID = "X-User-Id";
    public static final String USER_ROLES = "X-User-Roles";
    public static final String USER_EMAIL = "X-User-Email";
}
