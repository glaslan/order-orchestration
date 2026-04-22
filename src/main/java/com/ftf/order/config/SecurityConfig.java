package com.ftf.order.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // Exempt server-to-server endpoints from CSRF — they carry no session cookie.
            // All browser-facing POST endpoints (addToCart, removeFromCart, checkout) remain protected.
            .csrf(csrf -> csrf.ignoringRequestMatchers(
                    "/auth/session",
                    "/orders",
                    "/api/orders/sync",
                    "/has"))
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }
}
