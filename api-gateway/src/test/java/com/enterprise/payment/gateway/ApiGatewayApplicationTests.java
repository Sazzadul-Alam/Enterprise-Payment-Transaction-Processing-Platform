package com.enterprise.payment.gateway;

import com.enterprise.payment.common.constant.HeaderConstants;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ApiGatewayApplicationTests {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    @DisplayName("Should load application context successfully")
    void contextLoads() {
        assertThat(webTestClient).isNotNull();
    }

    @Test
    @DisplayName("Should return actuator health UP with correlation ID injected")
    void shouldReturnActuatorHealth() {
        webTestClient.get()
                .uri("/actuator/health")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().exists(HeaderConstants.CORRELATION_ID)
                .expectBody()
                .jsonPath("$.status").isEqualTo("UP");
    }
}
