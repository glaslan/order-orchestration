package com.ftf.order;

import lombok.Getter;
import lombok.Setter;

public class Category {
    @Getter @Setter private String name;
    @Getter @Setter private int id;

    public Category(String name, int id) {
        this.name = name;
    }
}