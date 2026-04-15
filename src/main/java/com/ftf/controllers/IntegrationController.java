package com.ftf.controllers;

import java.util.HashMap;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ftf.order.Item;

import jakarta.servlet.http.HttpSession;

@RestController
public class IntegrationController {
    
    // TODO Will probably need to change the names of the routes 



    // Customer team integration Routes
    // --------------------------------
    @PostMapping("/api/customer/send-order")
    public HashMap<String, Object> SendOrder(HttpSession session) {
        HashMap<String, Item> cart = (HashMap<String, Item>)session.getAttribute("cart");
        if (cart == null) {
            // don't want to send anything if the customer has no cart
            return null;
        }

        HashMap<String, Object> response = new HashMap<String, Object>();

        // TODO get these value to put in the reponse
        // response.put("OrderID", orderId);
        // response.put("CustomerID", customerId);
        // response.put("Items", cart);
        // response.put("Timestamp", timestamp);
        // reposne.put("Pickup", pickup);
        
        return response;
    }

    @GetMapping("/api/customer/order-status")
    public void OrderStatus(HttpSession session, @RequestBody HashMap<String, Object> request) {
        boolean status = (boolean)request.get("Status");
        int orderid = (int)request.get("OrderID");

        if (status == true) {
            this.NotifyInventory(session);
            session.setAttribute("cart", null);
        }
    }





    // Inventory team integration Routes
    // ---------------------------------
    @GetMapping("/api/inventory/get-update")
    public String UpdateDB() {
        // TODO Talk with inventory team about how they are sending data and how we will udate our clone 
        return "success";
    }

    @PostMapping("/api/inventory/send-update")
    public HashMap<String, Item> NotifyInventory(HttpSession session) {

        // We will send the cart to inventory and based on the items and quantities they will decrement from their stock
        HashMap<String, Item> cart = (HashMap<String, Item>)session.getAttribute("cart");
        if (cart == null) {
            // inventory will have to handle this
            return null;
        }

        // returns cart should be in JSON format
        return cart;
    }







    // Delivery team integration Routes
    // --------------------------------
    @PostMapping("/api/delivery/send-order")
    public void SendDeliveryOrder(HttpSession session) {
        // TODO Talk with delivery team about what they want from us
    }
}
