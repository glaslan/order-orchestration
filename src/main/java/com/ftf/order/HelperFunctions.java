package com.ftf.order;

import java.util.HashMap;

import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import jakarta.servlet.http.HttpSession;

// This class will hold a bunch of helper functions that can be used across the application
// that do not really belong in a class but Java is class based so...
public class HelperFunctions {

    public void SendOrderManifest(String url, HttpSession session, boolean pickup) {
        HashMap<String, InventoryItem> cart = (HashMap<String, InventoryItem>)session.getAttribute("cart");
        if (cart == null) {
            return;
        }

        // create order manifest to send
        Order orderManifest = new Order();
        // orderManifest.setOrderId(1234);
        orderManifest.setCustomerId((int)session.getAttribute("customerId"));
        orderManifest.setTimestamp(java.time.LocalDateTime.now().toString());
        orderManifest.setPickup(pickup);
        orderManifest.setItems(cart);

        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> response = restTemplate.postForEntity(url, orderManifest, String.class);
    }

}
