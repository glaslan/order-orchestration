package com.ftf.controllers;

import java.util.ArrayList;
import java.util.HashMap;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.ftf.order.InventoryItem;
import com.ftf.order.InventorySyncLog;
import com.ftf.order.InventorySyncService;

import jakarta.servlet.http.HttpSession;

@RestController
public class IntegrationController {

    private final InventorySyncService inventorySyncService;

    public IntegrationController(InventorySyncService inventorySyncService) {
        this.inventorySyncService = inventorySyncService;
    }

    // Customer team integration Routes
    // --------------------------------
    @PostMapping("/api/customer/send-order")
    public HashMap<String, Object> SendOrder(HttpSession session) {
        HashMap<String, InventoryItem> cart = (HashMap<String, InventoryItem>) session.getAttribute("cart");
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
        boolean status = (boolean) request.get("Status");
        int orderid = (int) request.get("OrderID");

        // if (Status == true) {
        //     this.NotifyInventory(session);
        //     session.setAttribute("cart", null);
        // }
    }


    @PostMapping("/has")
    public boolean Has(HttpSession session, @RequestBody ArrayList<Item> request) {
        

        for (Item item : request) {
            // TODO Check if item quantity availabe in database
            // get item from database
            // if (item.getQuantity() < dbItem.getQuantity()) {
            // return false;
            // }
        }

        return true;
    }


    // Inventory team integration Routes
    // ---------------------------------
    @PostMapping("/api/orders/sync")
    public InventorySyncLog syncInventory() {
        return inventorySyncService.syncInventory();
    }

    @PostMapping("/api/inventory/send-update")
    public HashMap<String, InventoryItem> NotifyInventory(HttpSession session) {

        // We will send the cart to inventory and based on the items and quantities they
        // will decrement from their stock
        HashMap<String, InventoryItem> cart = (HashMap<String, InventoryItem>) session.getAttribute("cart");
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
