package com.ftf.order.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // Exempt server-to-server endpoints from CSRF — they carry no session cookie.
            // All browser-facing POST endpoints (addToCart, removeFromCart, checkout) remain protected.
            .csrf(csrf -> csrf
                    .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                    // Spring Security 6 default XOR-masks the token before comparing, which breaks
                    // SPA fetch() calls that read the raw XSRF-TOKEN cookie and send it as-is.
                    // CsrfTokenRequestAttributeHandler skips the masking so cookie value == header value.
                    .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
                    .ignoringRequestMatchers(
                    "/auth/session",
                    "/orders",
                    "/api/orders/sync",
                    "/has"))
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }
}
