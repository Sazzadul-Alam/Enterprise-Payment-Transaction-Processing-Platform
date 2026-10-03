package com.enterprise.payment.common;

import com.enterprise.payment.common.dto.ApiErrorResponse;
import com.enterprise.payment.common.dto.ApiResponse;
import com.enterprise.payment.common.exception.ResourceNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;

class CommonLibraryTest {

    @Test
    @DisplayName("Should create successful ApiResponse")
    void shouldCreateSuccessfulApiResponse() {
        ApiResponse<String> response = ApiResponse.success("test-payload", "Operation successful", "corr-123");

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData()).isEqualTo("test-payload");
        assertThat(response.getMessage()).isEqualTo("Operation successful");
        assertThat(response.getCorrelationId()).isEqualTo("corr-123");
        assertThat(response.getTimestamp()).isNotNull();
    }

    @Test
    @DisplayName("Should create ApiErrorResponse")
    void shouldCreateApiErrorResponse() {
        ApiErrorResponse error = ApiErrorResponse.of("NOT_FOUND", "Item not found", "/api/v1/test", 404, "corr-456");

        assertThat(error.isSuccess()).isFalse();
        assertThat(error.getErrorCode()).isEqualTo("NOT_FOUND");
        assertThat(error.getMessage()).isEqualTo("Item not found");
        assertThat(error.getPath()).isEqualTo("/api/v1/test");
        assertThat(error.getStatus()).isEqualTo(404);
        assertThat(error.getCorrelationId()).isEqualTo("corr-456");
    }

    @Test
    @DisplayName("Should create ResourceNotFoundException with correct HTTP status and code")
    void shouldCreateResourceNotFoundException() {
        ResourceNotFoundException ex = new ResourceNotFoundException("Payment", "tx-999");

        assertThat(ex.getErrorCode()).isEqualTo("RESOURCE_NOT_FOUND");
        assertThat(ex.getHttpStatus()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(ex.getMessage()).isEqualTo("Payment with ID 'tx-999' was not found");
    }
}
