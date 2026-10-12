package com.enterprise.payment.user.util;

import com.enterprise.payment.user.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
@RequiredArgsConstructor
public class AccountNumberGenerator {

    private final AccountRepository accountRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * Generates a unique 12-digit account number starting with "10".
     */
    public String generateUniqueAccountNumber() {
        for (int attempt = 0; attempt < 10; attempt++) {
            StringBuilder sb = new StringBuilder("10");
            for (int i = 0; i < 10; i++) {
                sb.append(secureRandom.nextInt(10));
            }
            String candidate = sb.toString();
            if (!accountRepository.existsByAccountNumber(candidate)) {
                return candidate;
            }
        }
        // Fallback with timestamp suffix if loop exhausted
        return "10" + (System.currentTimeMillis() % 1_000_000_0000L);
    }
}
