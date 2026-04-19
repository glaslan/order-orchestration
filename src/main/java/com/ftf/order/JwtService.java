package com.ftf.order;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.Key;

@Service
public class JwtService {

    private final Key key;

    // Use SecretKeySpec directly so we aren't blocked by jjwt's minimum key-length
    // enforcement — the customer team signs with a short plaintext secret via Node's
    // jsonwebtoken library, which has no such restriction.
    public JwtService(@Value("${jwt.secret}") String secret) {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        this.key = new SecretKeySpec(keyBytes, "HmacSHA256");
    }

    public CustomerInfo parse(String token) {
        Claims claims = Jwts.parser()
                .verifyWith((javax.crypto.SecretKey) key)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        CustomerInfo info = new CustomerInfo();
        info.setId(claims.get("id", String.class));
        info.setName(claims.get("name", String.class));
        info.setEmail(claims.get("email", String.class));
        info.setPhone(claims.get("phone", String.class));
        info.setRole(claims.get("role", String.class));
        return info;
    }

    // Extracts customer from Authorization: Bearer <token> header; returns null on any failure.
    public CustomerInfo extractFromHeader(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            return null;
        }
        try {
            return parse(authorizationHeader.substring(7));
        } catch (Exception e) {
            return null;
        }
    }
}
