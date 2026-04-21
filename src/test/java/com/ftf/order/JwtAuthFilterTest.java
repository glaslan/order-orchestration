package com.ftf.order;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthFilterTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private FilterChain filterChain;

    private JwtAuthFilter filter;

    @BeforeEach
    void setUp() {
        filter = new JwtAuthFilter(jwtService);
        SecurityContextHolder.clearContext();
    }

    @Test
    void sessionCustomerShortCircuitsFilter() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        CustomerInfo sessionCustomer = new CustomerInfo();
        sessionCustomer.setId("cust-session");
        request.getSession(true).setAttribute("customer", sessionCustomer);

        filter.doFilterInternal(request, response, filterChain);

        assertThat(request.getAttribute(JwtAuthFilter.CUSTOMER_ATTR)).isSameAs(sessionCustomer);
        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtService);
    }

    @Test
    void missingAuthHeaderContinuesChain() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void validBearerTokenSetsCustomerAndSecurityContext() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.addHeader("Authorization", "Bearer valid.token.here");

        CustomerInfo customer = new CustomerInfo();
        customer.setId("cust-1");
        customer.setRole("admin");
        when(jwtService.parse("valid.token.here")).thenReturn(customer);

        filter.doFilterInternal(request, response, filterChain);

        assertThat(request.getAttribute(JwtAuthFilter.CUSTOMER_ATTR)).isSameAs(customer);
        var auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.getAuthorities()).anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void expiredTokenReturns401WithTokenExpiredError() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.addHeader("Authorization", "Bearer expired.token");

        when(jwtService.parse("expired.token"))
                .thenThrow(new InvalidTokenException(InvalidTokenException.Reason.EXPIRED, "expired", null));

        filter.doFilterInternal(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains("TOKEN_EXPIRED");
        verifyNoInteractions(filterChain);
    }

    @Test
    void invalidTokenReturns401WithInvalidTokenError() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.addHeader("Authorization", "Bearer bad.token");

        when(jwtService.parse("bad.token"))
                .thenThrow(new InvalidTokenException(InvalidTokenException.Reason.MALFORMED, "bad", null));

        filter.doFilterInternal(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains("INVALID_TOKEN");
        verifyNoInteractions(filterChain);
    }
}
