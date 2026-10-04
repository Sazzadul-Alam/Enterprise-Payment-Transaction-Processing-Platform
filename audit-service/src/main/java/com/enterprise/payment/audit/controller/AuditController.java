package com.enterprise.payment.audit.controller;

import com.enterprise.payment.common.constant.HeaderConstants;
import com.enterprise.payment.common.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/audit")
public class AuditController {

    @GetMapping("/status")
    public ResponseEntity<ApiResponse<Map<String, String>>> getStatus(
            @RequestHeader(value = HeaderConstants.CORRELATION_ID, required = false) String correlationId) {
        Map<String, String> status = Map.of(
                "service", "audit-service",
                "status", "OPERATIONAL"
        );
        return ResponseEntity.ok(ApiResponse.success(status, "Audit service operational", correlationId));
    }
}
