package com.ftf.controllers;

import java.util.HashMap;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.ftf.order.InventoryItem;
import com.ftf.order.HelperFunctions;
import com.ftf.order.Item;

import jakarta.servlet.http.HttpSession;

@Controller
public class OrderController {

    @PostMapping("/addToCart")

    public ResponseEntity<String> AddToCart(@RequestParam String name, @RequestParam double price, @RequestParam int quantity, @RequestParam int itemId, HttpSession session) {
        HashMap<String, InventoryItem> cart = (HashMap<String, InventoryItem>)session.getAttribute("cart");

    // public ResponseEntity<String> AddToCart(@RequestParam String name, @RequestParam double price, @RequestParam int quantity, @RequestParam int itemId, @RequestParam String category, HttpSession session) {
    //     HashMap<String, Item> cart = (HashMap<String, Item>)session.getAttribute("cart");

        if (cart == null) {
            cart = new HashMap<String, InventoryItem>();
        }

        InventoryItem cartItem = new InventoryItem();
        cartItem.setName(name);
        cartItem.setPrice(java.math.BigDecimal.valueOf(price));
        cartItem.setQuantity(quantity);
        cartItem.setSourceItemId((long) itemId);
        cart.put(name, cartItem);
        // TODO Check if item quantity is available in the database

        // cart.put(name, new Item(name, price, quantity, itemId, category));

        session.setAttribute("cart", cart);

        // TODO also need to add it to the order manifest db


        return ResponseEntity.ok("Item added to cart");
    }

    @PostMapping("/removeFromCart")
    public ResponseEntity<String> RemoveFromCart(@RequestParam String name, @RequestParam int quantity, HttpSession session) {
        HashMap<String, InventoryItem> cart = (HashMap<String, InventoryItem>)session.getAttribute("cart");

    // public ResponseEntity<String> RemoveFromCart(@RequestParam String name, @RequestParam int quantity, HttpSession session) {
    //     HashMap<String, Item> cart = (HashMap<String, Item>)session.getAttribute("cart");

        if (cart == null) {
            return ResponseEntity.badRequest().body("No cart found");
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

        return ResponseEntity.ok("Item removed from cart");
    }

    @GetMapping("/getCart")
    public ResponseEntity<HashMap<String, Item>> GetCart(HttpSession session) {
        HashMap<String, Item> cart = (HashMap<String, Item>)session.getAttribute("cart");
        if (cart == null) {
            cart = new HashMap<String, Item>();
        }

        return ResponseEntity.ok(cart);
    }

    // this will send the customers cart to the customer team
    @PostMapping("/sendOrder")
    public void SendOrder(HttpSession session) {
        HelperFunctions helper = new HelperFunctions();
        
        // TODO idk the api route
        helper.SendOrderManifest("http://127.0.0.1:8080/api/customer/getOrder", session, false);

        // clear cart
        HashMap<String, Item> cart = (HashMap<String, Item>)session.getAttribute("cart");
        cart.clear();
        session.setAttribute("cart", cart);
    }
}
