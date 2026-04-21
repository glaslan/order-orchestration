package com.ftf.order.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import com.ftf.order.model.Category;
import com.ftf.order.model.InventoryItem;
import com.ftf.order.model.Order;
import com.ftf.order.model.OrderItem;
import com.ftf.order.model.SoldItem;
import com.ftf.order.repository.InventoryItemRepository;

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

    public void SendSoldItems(String url, List<OrderItem> orderItems, InventoryItemRepository inventoryItemRepository, RestTemplate restTemplate) {
        ArrayList<SoldItem> soldItems = new ArrayList<>();
        try {
            
            orderItems.forEach(oi -> {
                
                InventoryItem inventoryItem = inventoryItemRepository.findById(oi.getInventoryItemId()).orElse(null);
                
                if (inventoryItem != null) {
                    SoldItem item = new SoldItem(
                        oi.getProductName(),
                        new Category(inventoryItem.getCategoryId(), inventoryItem.getCategoryName()),
                        oi.getQuantity(),
                        oi.getPrice(),
                        oi.getInventoryItemId()
                    );
                    soldItems.add(item);
                } else {
                    SoldItem item = new SoldItem(
                        oi.getProductName(),
                        new Category(Long.valueOf(0), "Unknown"),
                        oi.getQuantity(),
                        oi.getPrice(),
                        oi.getInventoryItemId()
                    );
                    soldItems.add(item);
                }
                
            });

            restTemplate.postForObject("http://134.122.40.121:5180/api/inventory_intelligence/inventory/sold_items", soldItems, Void.class);
        } catch (Exception e) {
            System.err.println("Failed to send sold items to inventory intelligence: " + e.getMessage());
        }
    }

}
