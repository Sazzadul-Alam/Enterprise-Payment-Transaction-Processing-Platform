package com.enterprise.payment.auth.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "security.lockout")
@Getter
@Setter
public class LockoutProperties {

    /** Maximum failed login attempts before locking account */
    private int maxAttempts = 5;

    /** Lockout duration in minutes */
    private int lockDurationMinutes = 15;
}
