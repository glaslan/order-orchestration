package com.ftf.order.model;

import jakarta.servlet.http.HttpSession;
import lombok.Data;

@Data
public class CustomerInfo {
    private String id;
    private String name;
    private String email;
    private String phone;
    private String role;

    public static CustomerInfo getCustomer(HttpSession session) {
        return (CustomerInfo) session.getAttribute("customer");
    }
}
