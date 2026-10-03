package com.enterprise.payment.gateway.filter;

import com.enterprise.payment.common.constant.HeaderConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter implements WebFilter {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String correlationId = request.getHeaders().getFirst(HeaderConstants.CORRELATION_ID);

        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
            log.debug("Generated new Correlation-ID: {} for path: {}", correlationId, request.getPath());
        } else {
            log.debug("Forwarding existing Correlation-ID: {} for path: {}", correlationId, request.getPath());
        }

        final String finalCorrelationId = correlationId;

        ServerHttpRequest mutatedRequest = request.mutate()
                .header(HeaderConstants.CORRELATION_ID, finalCorrelationId)
                .build();

        exchange.getResponse().getHeaders().add(HeaderConstants.CORRELATION_ID, finalCorrelationId);

        return chain.filter(exchange.mutate().request(mutatedRequest).build());
    }
}
