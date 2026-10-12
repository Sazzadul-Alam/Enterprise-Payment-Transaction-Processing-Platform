package com.enterprise.payment.user.dto;

import com.enterprise.payment.user.entity.enums.AddressType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddressResponse {

    private UUID id;
    private UUID userId;
    private AddressType addressType;
    private String street;
    private String city;
    private String state;
    private String country;
    private String postalCode;
    private boolean primary;
    private OffsetDateTime createdAt;
}
