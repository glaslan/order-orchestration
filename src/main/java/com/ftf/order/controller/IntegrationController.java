package com.ftf.order.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.ftf.order.model.CartItem;
import com.ftf.order.model.CustomerInfo;
import com.ftf.order.model.InventoryItem;
import com.ftf.order.model.InventorySyncLog;
import com.ftf.order.model.OrderManifest;
import com.ftf.order.repository.CartItemRepository;
import com.ftf.order.repository.InventoryItemRepository;
import com.ftf.order.service.CheckoutService;
import com.ftf.order.service.InventorySyncService;
import com.ftf.order.service.JwtService;

import jakarta.servlet.http.HttpSession;

@RestController
public class IntegrationController {

    private final InventorySyncService inventorySyncService;
    private final CheckoutService checkoutService;
    private final CartItemRepository cartItemRepository;
    private final InventoryItemRepository inventoryItemRepository;
    private final JwtService jwtService;

    public IntegrationController(InventorySyncService inventorySyncService,
                                 CheckoutService checkoutService,
                                 CartItemRepository cartItemRepository,
                                 InventoryItemRepository inventoryItemRepository,
                                 JwtService jwtService) {
        this.inventorySyncService = inventorySyncService;
        this.checkoutService = checkoutService;
        this.cartItemRepository = cartItemRepository;
        this.inventoryItemRepository = inventoryItemRepository;
        this.jwtService = jwtService;
    }

    // Customer team integration Routes
    // --------------------------------

    // Called by the customer team when a subscription order should be created.
    // They provide customerId + subscriptionId; we build the order and charge billing.
    @PostMapping("/orders")
    public ResponseEntity<?> CreateOrder(@RequestBody HashMap<String, Object> body) {
        String customerId = (String) body.get("customerId");
        String subscriptionId = (String) body.get("subscriptionId");

        if (customerId == null || subscriptionId == null) {
            return ResponseEntity.badRequest().body("customerId and subscriptionId are required");
        }

        List<CartItem> cartItems = cartItemRepository.findByCustomerId(customerId);
        if (cartItems.isEmpty()) {
            return ResponseEntity.badRequest().body("No cart found for customer");
        }

        CustomerInfo customer = new CustomerInfo();
        customer.setId(customerId);

        try {
            OrderManifest manifest = checkoutService.checkout(customer, subscriptionId, false);
            return ResponseEntity.ok(manifest);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }


    @PostMapping("/has")
    public boolean Has(HttpSession session, @RequestBody ArrayList<InventoryItem> request) {
        for (InventoryItem item : request) {
            // TODO Check if item quantity available in database
            // InventoryItem dbItem = inventoryItemRepository.findBySourceItemId(item.getSourceItemId()).orElse(null);
            // if (dbItem == null || dbItem.getQuantity() < item.getQuantity()) return false;
        }
        return true;
    }


    // Inventory team integration Routes
    // ---------------------------------
    @PostMapping("/api/orders/sync")
    public InventorySyncLog syncInventory() {
        return inventorySyncService.syncInventory();
    }

    // Delivery team integration Routes
    // --------------------------------
    @PostMapping("/api/delivery/send-order")
    public void SendDeliveryOrder(HttpSession session) {
        // TODO confirm delivery team's endpoint — handled automatically through CheckoutService
    }
}
