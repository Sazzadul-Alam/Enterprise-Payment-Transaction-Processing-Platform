package com.enterprise.payment.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiErrorResponse {

    private boolean success;
    private String errorCode;
    private String message;
    private String path;
    private int status;
    private String correlationId;
    private Map<String, List<String>> validationErrors;
    @Builder.Default
    private Instant timestamp = Instant.now();

    public static ApiErrorResponse of(String errorCode, String message, String path, int status, String correlationId) {
        return ApiErrorResponse.builder()
                .success(false)
                .errorCode(errorCode)
                .message(message)
                .path(path)
                .status(status)
                .correlationId(correlationId)
                .timestamp(Instant.now())
                .build();
    }
}
