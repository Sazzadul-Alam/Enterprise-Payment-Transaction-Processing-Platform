package com.enterprise.payment.auth.security;

import com.enterprise.payment.common.constant.HeaderConstants;
import com.enterprise.payment.common.dto.ApiErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException) throws IOException {

        String correlationId = request.getHeader(HeaderConstants.CORRELATION_ID);

        ApiErrorResponse errorResponse = ApiErrorResponse.of(
                "ACCESS_DENIED",
                "Access is denied for this resource: " + accessDeniedException.getMessage(),
                request.getRequestURI(),
                HttpServletResponse.SC_FORBIDDEN,
                correlationId
        );

        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), errorResponse);
    }
}
