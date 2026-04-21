package com.ftf.order.model;

import java.util.List;
import java.util.Map;

public class DeliveryOrder {

    private Long orderId;
    private String customerId;
    private String timestamp;
    private boolean pickup;
    private List<Map<String, Object>> items;

    public DeliveryOrder(Long orderId, String customerId, String timestamp, boolean pickup, List<Map<String, Object>> items) {
        this.orderId = orderId;
        this.customerId = customerId;
        this.timestamp = timestamp;
        this.pickup = pickup;
        this.items = items;
    }
    
}