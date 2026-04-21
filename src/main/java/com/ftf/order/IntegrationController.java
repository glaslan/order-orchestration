package com.ftf.order;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
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
    // The customer team must send their JWT so we can verify identity and forward it to other teams.
    @PostMapping("/orders")
    public ResponseEntity<?> CreateOrder(@RequestBody HashMap<String, Object> body,
                                         HttpServletRequest request) {
        CustomerInfo customer = (CustomerInfo) request.getAttribute(JwtAuthFilter.CUSTOMER_ATTR);
        if (customer == null) return ResponseEntity.status(401).body("Authentication required");

        String subscriptionId = (String) body.get("subscriptionId");
        if (subscriptionId == null) {
            return ResponseEntity.badRequest().body("subscriptionId is required");
        }

        List<CartItem> cartItems = cartItemRepository.findByCustomerId(customer.getId());
        if (cartItems.isEmpty()) {
            return ResponseEntity.badRequest().body("No cart found for customer");
        }

        try {
            OrderManifest manifest = checkoutService.checkout(customer, subscriptionId,
                    request.getHeader("Authorization"), false);
            return ResponseEntity.ok(manifest);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/api/customer/send-order")
    public HashMap<String, Object> SendOrder(HttpSession session) {
        HashMap<String, InventoryItem> cart = (HashMap<String, InventoryItem>) session.getAttribute("cart");
        if (cart == null) {
            return null;
        }

        HashMap<String, Object> response = new HashMap<String, Object>();

        // TODO get these values to put in the response
        // response.put("OrderID", orderId);
        // response.put("CustomerID", customerId);
        // response.put("Items", cart);
        // response.put("Timestamp", timestamp);
        // response.put("Pickup", pickup);

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

    @PostMapping("/api/inventory/send-update")
    public ResponseEntity<?> NotifyInventory(HttpSession session, HttpServletRequest request) {
        CustomerInfo customer = jwtService.extractFromHeader(request.getHeader("Authorization"));
        if (customer == null) {
            customer = (CustomerInfo) session.getAttribute("customer");
        }
        if (customer == null) return ResponseEntity.status(401).body("Authentication required");

        List<CartItem> cartItems = cartItemRepository.findByCustomerId(customer.getId());
        return ResponseEntity.ok(cartItems);
    }

    // Delivery team integration Routes
    // --------------------------------
    @PostMapping("/api/delivery/send-order")
    public void SendDeliveryOrder(HttpSession session) {
        // TODO confirm delivery team's endpoint — handled automatically through CheckoutService
    }
}
