package com.ftf.order;

import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String SECRET = "test-secret-key";
    private JwtService jwtService;
    private Key key;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(SECRET);
        key = new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }

    private String buildToken(String id, String name, String email, String phone, String role, Date expiry) {
        var builder = Jwts.builder()
                .claim("id", id)
                .claim("name", name)
                .claim("email", email)
                .claim("phone", phone)
                .claim("role", role)
                .signWith(key);
        if (expiry != null) builder.expiration(expiry);
        return builder.compact();
    }

    @Test
    void parsesAllClaimsFromValidToken() {
        String token = buildToken("cust-1", "Alice", "alice@example.com", "555-1234", "USER", null);

        CustomerInfo result = jwtService.parse(token);

        assertThat(result.getId()).isEqualTo("cust-1");
        assertThat(result.getName()).isEqualTo("Alice");
        assertThat(result.getEmail()).isEqualTo("alice@example.com");
        assertThat(result.getPhone()).isEqualTo("555-1234");
        assertThat(result.getRole()).isEqualTo("USER");
    }

    @Test
    void throwsExpiredOnExpiredToken() {
        Date past = new Date(System.currentTimeMillis() - 60_000);
        String token = buildToken("cust-1", "Alice", "alice@example.com", null, null, past);

        assertThatThrownBy(() -> jwtService.parse(token))
                .isInstanceOf(InvalidTokenException.class)
                .extracting(e -> ((InvalidTokenException) e).getReason())
                .isEqualTo(InvalidTokenException.Reason.EXPIRED);
    }

    @Test
    void throwsInvalidSignatureOnWrongSecret() {
        Key wrongKey = new SecretKeySpec("wrong-secret-key".getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        String token = Jwts.builder().claim("id", "x").signWith(wrongKey).compact();

        assertThatThrownBy(() -> jwtService.parse(token))
                .isInstanceOf(InvalidTokenException.class)
                .extracting(e -> ((InvalidTokenException) e).getReason())
                .isEqualTo(InvalidTokenException.Reason.INVALID_SIGNATURE);
    }

    @Test
    void throwsMalformedOnGarbageToken() {
        assertThatThrownBy(() -> jwtService.parse("not.a.jwt"))
                .isInstanceOf(InvalidTokenException.class)
                .extracting(e -> ((InvalidTokenException) e).getReason())
                .isEqualTo(InvalidTokenException.Reason.MALFORMED);
    }

    @Test
    void extractFromHeaderReturnsNullWhenHeaderMissing() {
        assertThat(jwtService.extractFromHeader(null)).isNull();
    }

    @Test
    void extractFromHeaderReturnsNullWhenTokenInvalid() {
        assertThat(jwtService.extractFromHeader("Bearer bad.token.here")).isNull();
    }

    @Test
    void extractFromHeaderReturnsCustomerOnValidToken() {
        String token = buildToken("cust-2", "Bob", "bob@example.com", null, null, null);

        CustomerInfo result = jwtService.extractFromHeader("Bearer " + token);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("cust-2");
    }
}
