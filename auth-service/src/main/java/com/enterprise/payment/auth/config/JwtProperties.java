package com.enterprise.payment.auth.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "jwt")
@Getter
@Setter
public class JwtProperties {

    /** Secret key for HMAC-SHA256 signing (at least 256 bits) */
    private String secret = "9a4f2c8d3e7b1a5f6c8d2e4a7b9c1d3e5f7a8b0c2d4e6f8a1b3c5d7e9f0a2b4c";

    /** Access token expiration time in milliseconds (default: 15 minutes) */
    private long accessTokenExpiration = 900_000L;

    /** Refresh token expiration time in milliseconds (default: 7 days) */
    private long refreshTokenExpiration = 604_800_000L;

    /** JWT issuer */
    private String issuer = "enterprise-auth-service";
}
