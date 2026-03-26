package com.ftf.order;

public class Item {

    // name is used as the key in the JSON/Hashmap
    private double price;
    private int quantity;

    public Item(double price, int quantity) {
        this.price = price;
        this.quantity = quantity;
    }


    public double getPrice() {
        return price;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

}
