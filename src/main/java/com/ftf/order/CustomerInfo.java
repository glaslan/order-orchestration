package com.ftf.order;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.Data;

@Data
public class CustomerInfo {
    private String id;
    private String name;
    private String email;
    private String phone;
    private String role;

    public static CustomerInfo getDebugCustomer() {
        CustomerInfo debugCustomer = new CustomerInfo();
        debugCustomer.setId("123456789");
        debugCustomer.setName("John Doe");
        debugCustomer.setEmail("johnd@gmail.com");
        debugCustomer.setPhone("123-456-7890");
        debugCustomer.setRole("customer");
        return debugCustomer;
    }

    // Checks session first (set via POST /auth/session), then falls back to Authorization header.
    public static CustomerInfo getCustomer(HttpSession session, HttpServletRequest request, JwtService jwtService) {
        if (OrderApplication.DEBUG_MODE) {
            return CustomerInfo.getDebugCustomer();
        }
        CustomerInfo fromSession = (CustomerInfo) session.getAttribute("customer");
        if (fromSession != null) return fromSession;
        return jwtService.extractFromHeader(request.getHeader("Authorization"));
    }
}

