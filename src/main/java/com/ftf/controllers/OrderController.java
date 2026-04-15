package com.ftf.controllers;

import java.util.HashMap;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.ftf.order.InventoryItem;

import jakarta.servlet.http.HttpSession;

@Controller
public class OrderController {

    @PostMapping("/addToCart")
    public String AddToCart(@RequestParam String name, @RequestParam double price, @RequestParam int quantity, @RequestParam int itemId, HttpSession session) {
        HashMap<String, InventoryItem> cart = (HashMap<String, InventoryItem>)session.getAttribute("cart");
        if (cart == null) {
            cart = new HashMap<String, InventoryItem>();
        }

        InventoryItem cartItem = new InventoryItem();
        cartItem.setName(name);
        cartItem.setPrice(java.math.BigDecimal.valueOf(price));
        cartItem.setQuantity(quantity);
        cartItem.setSourceItemId((long) itemId);
        cart.put(name, cartItem);
        session.setAttribute("cart", cart);

        // TODO also need to add it to the order manifest db

        // TODO I probably need to change this 
        return "success";
    }

    @PostMapping("/removeFromCart")
    public String RemoveFromCart(@RequestParam String name, @RequestParam int quantity, HttpSession session) {
        HashMap<String, InventoryItem> cart = (HashMap<String, InventoryItem>)session.getAttribute("cart");
        if (cart == null) {
            return "failure";
        }

        InventoryItem item = cart.get(name);
        if (item != null) {
            // if the quantity is less then the amount of cart then decrement the amount in the cart
            if (item.getQuantity() > quantity) {
                item.setQuantity(item.getQuantity() - quantity);
                cart.replace(name, item);
            }
            // if quantity in cart is equal or less than amount from params then we can just remove it
            else {
                cart.remove(name);
            }

        }
        session.setAttribute("cart", cart);

        // TODO also need to remove it from the order manifest db

        return "success";
    }
}
