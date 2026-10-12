package com.enterprise.payment.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthUserResponse {

    private UUID id;
    private String email;
    private boolean active;
    private Set<String> roles;
    private OffsetDateTime createdAt;
    private OffsetDateTime lastLoginAt;
}
