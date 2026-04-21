package com.ftf.order.model;

public class CartItemWithInventory {
    private final CartItem cartItem;
    private final InventoryItem inventoryItem;

    public CartItemWithInventory(CartItem cartItem, InventoryItem inventoryItem) {
        this.cartItem = cartItem;
        this.inventoryItem = inventoryItem;
    }

    public CartItem getCartItem() {
        return cartItem;
    }

    public InventoryItem getInventoryItem() {
        return inventoryItem;
    }
}