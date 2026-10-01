package com.ridelink.account.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

import org.junit.jupiter.api.Test;

import com.ridelink.account.model.Account;
import com.ridelink.account.model.AccountRole;
import com.ridelink.account.model.AccountStatus;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;

class JwtServiceTest {

    // Clearly fake test-only key: production uses the JWT_SECRET environment variable.
    private static final String TEST_SECRET = Base64.getEncoder().encodeToString(
            "0123456789abcdef0123456789abcdef".getBytes(StandardCharsets.UTF_8));

    @Test
    void signedTokenContainsIdentityRoleAndConfiguredExpiration() {
        JwtService jwtService = new JwtService(TEST_SECRET, 3600);
        Instant now = Instant.now();

        String token = jwtService.issueToken(account());
        Claims claims = jwtService.verifyToken(token);

        assertEquals("account-1", claims.getSubject());
        assertEquals("passenger@test.com", claims.get("email", String.class));
        assertEquals("PASSENGER", claims.get("role", String.class));
        assertEquals("ridelink-account-service", claims.getIssuer());
        assertNotNull(claims.getIssuedAt());
        assertNotNull(claims.getExpiration());
        assertEquals(3600, (claims.getExpiration().getTime() - claims.getIssuedAt().getTime()) / 1000);
        assertFalse(claims.containsKey("password"));
        assertFalse(claims.containsKey("phoneNumber"));
        assertFalse(claims.containsKey("mongodbUri"));
        assertFalse(claims.getExpiration().toInstant().isBefore(now));
    }

    @Test
    void rejectsWrongSigningKey() {
        JwtService issuer = new JwtService(TEST_SECRET, 3600);
        String otherTestSecret = Base64.getEncoder().encodeToString(
                "abcdef0123456789abcdef0123456789".getBytes(StandardCharsets.UTF_8));
        JwtService verifier = new JwtService(otherTestSecret, 3600);

        assertThrows(JwtException.class, () -> verifier.verifyToken(issuer.issueToken(account())));
    }

    @Test
    void rejectsWeakSecretAndNonPositiveExpiration() {
        String shortTestSecret = Base64.getEncoder().encodeToString("short".getBytes(StandardCharsets.UTF_8));

        assertThrows(IllegalStateException.class, () -> new JwtService(shortTestSecret, 3600));
        assertThrows(IllegalStateException.class, () -> new JwtService(TEST_SECRET, 0));
    }

    private Account account() {
        Instant now = Instant.parse("2026-09-30T10:00:00Z");
        Account account = new Account("Test Passenger", "passenger@test.com", "encoded-password",
                "0771234567", AccountRole.PASSENGER, AccountStatus.ACTIVE, now, now);
        account.setId("account-1");
        return account;
    }
}
