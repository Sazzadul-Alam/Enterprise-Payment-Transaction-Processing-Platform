package com.enterprise.payment.user.dto;

import com.enterprise.payment.user.entity.enums.KycStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateKycRequest {

    @NotNull(message = "KYC status is required")
    private KycStatus kycStatus;

    private String remarks;
}
