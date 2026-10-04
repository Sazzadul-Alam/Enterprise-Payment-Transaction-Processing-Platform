package com.enterprise.payment.notification;

import com.enterprise.payment.common.constant.HeaderConstants;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class NotificationServiceApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Should return notification service status with correlation ID")
    void shouldReturnNotificationServiceStatus() throws Exception {
        mockMvc.perform(get("/api/v1/notifications/status")
                        .header(HeaderConstants.CORRELATION_ID, "corr-notif-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.correlationId").value("corr-notif-123"))
                .andExpect(jsonPath("$.data.service").value("notification-service"))
                .andExpect(jsonPath("$.data.status").value("OPERATIONAL"));
    }

    @Test
    @DisplayName("Should return actuator health UP")
    void shouldReturnActuatorHealth() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }
}
