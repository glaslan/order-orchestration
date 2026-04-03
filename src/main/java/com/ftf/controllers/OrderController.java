package com.ftf.controllers;

import java.util.HashMap;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.ftf.order.Item;

import jakarta.servlet.http.HttpSession;

@Controller
public class OrderController {
    
    @GetMapping("/add-to-cart")
    public String AddToCart(@RequestParam String name, @RequestParam double price, @RequestParam int quantity, @RequestParam String unit, HttpSession session) {
        HashMap<String, Item> cart = (HashMap<String, Item>)session.getAttribute("cart");
        if (cart == null) {
            cart = new HashMap<String, Item>();
        }

        cart.put("name", new Item(price, quantity, unit));
        session.setAttribute("cart", cart);

        // TODO also need to add it to the order manifest db

        // TODO I probably need to change this 
        return "success";
    }
}
