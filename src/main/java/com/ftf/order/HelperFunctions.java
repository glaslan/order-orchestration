package com.ftf.order;

import java.util.ArrayList;
import java.util.HashMap;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import jakarta.servlet.http.HttpSession;

// This class will hold a bunch of helper functions that can be used across the application
// that do not really belong in a class but Java is class based so...
public class HelperFunctions {
    

    public void SyncDB(String url) {

        // make request to inventory intelligence teams api for all items
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<ArrayList<Item>> response = restTemplate.exchange(url, HttpMethod.GET, null, new ParameterizedTypeReference<ArrayList<Item>>() {});
        ArrayList<Item> items = response.getBody();

        // add each item to the database
        for (Item item : items) {

            // TODO Call query to add item to database
        }
                
    }

    public void SendOrderManifest(String url, HttpSession session, boolean pickup) {
        HashMap<String, Item> cart = (HashMap<String, Item>)session.getAttribute("cart");
        if (cart == null) {
            return;
        }

        // create order manifest to send
        Order orderManifest = new Order();
        // orderManifest.setOrderId(1234);
        orderManifest.setCustomerId((int)session.getAttribute("customerId"));
        orderManifest.setTimestamp(java.time.LocalDateTime.now().toString()); // need to ask if this the correct format I don't remember
        orderManifest.setPickup(pickup);
        orderManifest.setItems(cart);
    

        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> response = restTemplate.postForEntity(url, orderManifest, String.class);
    }

}
