package com.ftf.order.model;

import java.util.List;
import java.util.Map;

public class DeliveryOrder {

    private Long order_id;
    private String customer_id;
    private String request_timestamp;
    private boolean pickup;
    private List<Map<String, Object>> items;
    private String order_type;

    public DeliveryOrder(Long order_id, String customer_id, String request_timestamp, boolean pickup, List<Map<String, Object>> items) {
        this.order_id = order_id;
        this.customer_id = customer_id;
        this.request_timestamp = request_timestamp;
        this.pickup = pickup;
        this.items = items;
        this.order_type = "delivery";
    }
    
}