package com.ftf.order.model;

import java.util.HashMap;

import lombok.Getter;
import lombok.Setter;

public class Order {

    // class to compile information to send as order manifest
    @Getter @Setter private int orderId;
    @Getter @Setter private int customerId;
    @Getter @Setter private HashMap<String, InventoryItem> items;
    @Getter @Setter private String timestamp;
    @Getter @Setter private boolean pickup;

    // use setter's to set information
    public Order() {}

}
