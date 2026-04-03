package com.ftf.order;

import lombok.Getter;
import lombok.Setter;

public class Item {

    // name is used as the key in the JSON/Hashmap
    @Getter @Setter private double price;
    @Getter @Setter private int quantity;
    @Getter @Setter private String unit;

    public Item(double price, int quantity, String unit) {
        this.price = price;
        this.quantity = quantity;
        this.unit = unit;
    }

}


