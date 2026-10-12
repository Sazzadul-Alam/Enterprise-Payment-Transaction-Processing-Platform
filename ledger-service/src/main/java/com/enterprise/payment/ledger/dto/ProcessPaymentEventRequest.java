package com.enterprise.payment.ledger.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessPaymentEventRequest {

    @NotBlank(message = "Event ID is required")
    private String eventId;

    @NotBlank(message = "Event type is required")
    private String eventType;

    @NotNull(message = "Payment ID is required")
    private UUID paymentId;

    @NotNull(message = "Payer account ID is required")
    private UUID payerAccountId;

    @NotNull(message = "Payee account ID is required")
    private UUID payeeAccountId;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.0001", message = "Amount must be greater than zero")
    private BigDecimal amount;

    @NotBlank(message = "Currency is required")
    @Size(min = 3, max = 3, message = "Currency must be a 3-character ISO code")
    private String currency;

    private String description;
}
