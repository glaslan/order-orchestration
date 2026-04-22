package com.ftf.order.model;

import java.math.BigDecimal;

public class SoldItem {

    private String name;
    private Category category;
    private int quantity;
    private BigDecimal price;
    private Long id;

    public SoldItem(String name, Category category, int quantity, BigDecimal price, Long id) {
        this.name = name;
        this.category = category;
        this.quantity = quantity;
        this.price = price;
        this.id = id;
    }
    
}
