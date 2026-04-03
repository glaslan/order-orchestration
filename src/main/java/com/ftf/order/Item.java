package com.ftf.order;

import lombok.Getter;
import lombok.Setter;

public class Item {

    // name is used as the key in the JSON/Hashmap
    // Subject to change based on what invenotry team is passing/storing
    @Getter @Setter private double price;
    @Getter @Setter private int quantity;
    @Getter @Setter private int id;
    @Getter @Setter private String name;

    public Item(String name, double price, int quantity, int id) {
        this.name = name;
        this.price = price;
        this.quantity = quantity;
        this.id = id;
    }


}
