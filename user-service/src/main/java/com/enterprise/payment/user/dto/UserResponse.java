package com.enterprise.payment.user.dto;

import com.enterprise.payment.user.entity.enums.KycStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {

    private UUID id;
    private UUID authUserId;
    private String firstName;
    private String lastName;
    private String email;
    private String phoneNumber;
    private LocalDate dateOfBirth;
    private KycStatus kycStatus;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private List<AccountResponse> accounts;
    private List<AddressResponse> addresses;
}
