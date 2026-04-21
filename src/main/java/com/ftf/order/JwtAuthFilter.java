package com.ftf.order;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    // Request attribute key used by controllers to retrieve the resolved customer.
    public static final String CUSTOMER_ATTR = "jwt_customer";

    private final JwtService jwtService;

    public JwtAuthFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // Session-based path: browser UI stores CustomerInfo in session after POST /auth/session.
        HttpSession session = request.getSession(false);
        if (session != null && session.getAttribute("customer") != null) {
            request.setAttribute(CUSTOMER_ATTR, session.getAttribute("customer"));
            filterChain.doFilter(request, response);
            return;
        }

        // Header-based path: REST clients send Authorization: Bearer <token>.
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            // No token — let Spring Security's authorization rules decide if the endpoint is public.
            filterChain.doFilter(request, response);
            return;
        }

        try {
            CustomerInfo customer = jwtService.parse(authHeader.substring(7));

            // Make the customer available to controllers via request attribute.
            request.setAttribute(CUSTOMER_ATTR, customer);

            // Populate the Spring SecurityContext so .authenticated() rules work.
            String role = customer.getRole() != null ? customer.getRole().toUpperCase() : "USER";
            UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                    customer, null, List.of(new SimpleGrantedAuthority("ROLE_" + role)));
            SecurityContextHolder.getContext().setAuthentication(authToken);

            filterChain.doFilter(request, response);

        } catch (InvalidTokenException e) {
            SecurityContextHolder.clearContext();
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            String errorCode = e.getReason() == InvalidTokenException.Reason.EXPIRED
                    ? "TOKEN_EXPIRED" : "INVALID_TOKEN";
            response.getWriter().write("{\"error\":\"" + errorCode + "\"}");
        }
    }
}
