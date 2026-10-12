package com.enterprise.payment.auth.entity.enums;

/**
 * Role name constants mirrored as an enum for compile-time safety.
 * The canonical role names are stored as strings in the {@code roles} table.
 */
public enum RoleName {
    ROLE_USER,
    ROLE_ADMIN,
    ROLE_AUDITOR,
    ROLE_SERVICE
}
