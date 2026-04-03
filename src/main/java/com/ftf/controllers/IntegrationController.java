package com.ftf.controllers;

import java.util.HashMap;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

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

        // TODO get these value to put in the response 
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

    // @GetMapping
    // This route is from the inventory team and will send all of the items in their database
    // /api/inventory_intelligence/inventory/all_items

    // @GetMapping
    // this route from the inventory team will sends all items based on a certain type
    // /api/inventory_intelligence/inventory/get_items_by_type

    // @PostMapping
    // we will send the order information to this endpoint which will tell them to decrement the stock of the items 
    // /api/inventory_intelligence/inventory/sold_items








    // Delivery team integration Routes
    // --------------------------------
    @PostMapping("/api/delivery/send-order")
    public void SendDeliveryOrder(HttpSession session) {
        // TODO Talk with delivery team about what they want from us
    }



    // Functions for integration 
    public void InitializeDatabase() {

        // TODO change the IP address and port number to the one we are deploying on and the port inventory are using 
        String apiURL = "http://127.0.0.1:8080/api/inventory_intelligence/inventory/all_items";

        RestTemplate template = new RestTemplate();
        String response = template.getForObject(apiURL, String.class);

        // TODO init the database with all the items from the response 

    }
}