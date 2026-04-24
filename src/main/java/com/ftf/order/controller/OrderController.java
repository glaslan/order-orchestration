package com.ftf.order.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.ftf.order.model.CartItem;
import com.ftf.order.model.CustomerInfo;
import com.ftf.order.model.InventoryItem;
import com.ftf.order.model.OrderManifest;
import com.ftf.order.repository.CartItemRepository;
import com.ftf.order.repository.InventoryItemRepository;
import com.ftf.order.service.CheckoutService;
import com.ftf.order.service.JwtService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import static java.sql.DriverManager.println;

@Controller
public class OrderController {

    private final CartItemRepository cartItemRepository;
    private final InventoryItemRepository inventoryItemRepository;
    private final CheckoutService checkoutService;
    private final JwtService jwtService;

    public OrderController(CartItemRepository cartItemRepository,
                        InventoryItemRepository inventoryItemRepository,
                        CheckoutService checkoutService,
                        JwtService jwtService) {
        this.cartItemRepository = cartItemRepository;
        this.inventoryItemRepository = inventoryItemRepository;
        this.checkoutService = checkoutService;
        this.jwtService = jwtService;
    }

    @PostMapping("/auth/session")
    public ResponseEntity<String> setSession(@org.springframework.web.bind.annotation.RequestBody String token,
                                            HttpSession session,
                                            HttpServletRequest request) {
        try {
            CustomerInfo customer = jwtService.parse(token);
            session.invalidate();
            HttpSession fresh = request.getSession(true);
            fresh.setAttribute("customer", customer);
            return ResponseEntity.ok("Session established ID: " + fresh.getId() + fresh.getAttribute("customer"));
        } catch (Exception e) {
            return ResponseEntity.status(401).body("Authentication failed");
        }
    }

    @PostMapping("/auth/logout")
    public ResponseEntity<String> logout(HttpSession session) {
        session.invalidate();
        return ResponseEntity.ok("Logged out");
    }

    @PostMapping("/addToCart")
    public ResponseEntity<String> AddToCart(@RequestParam Long itemId,
                                            @RequestParam int quantity,
                                            HttpSession session) {

        CustomerInfo customer = CustomerInfo.getCustomer(session);
        if (customer == null) return ResponseEntity.status(401).body("Authentication required");

        // itemId from the UI is our internal inventory_item.id
        Optional<InventoryItem> invOpt = inventoryItemRepository.findById(itemId);
        if (invOpt.isEmpty() || !invOpt.get().isActive()) {
            return ResponseEntity.badRequest().body("Item not available");
        }
        InventoryItem inv = invOpt.get();

        Optional<CartItem> existing = cartItemRepository.findByCustomerIdAndInventoryItemId(
                customer.getId(), inv.getId());

        if (existing.isPresent()) {
            CartItem ci = existing.get();

            //validate that quantity entered is valid
            int newQuantity = ci.getQuantity() + quantity;
            if(quantity <= 0) return ResponseEntity.badRequest().body("Quantity must be at least 1");
            int invQuantity = inv.getQuantity();
            if(newQuantity > invQuantity) return ResponseEntity.badRequest().body("Only " + invQuantity +  " in stock");

            ci.setQuantity(ci.getQuantity() + quantity);
            cartItemRepository.save(ci);
        } else {
            if (quantity <= 0) return ResponseEntity.badRequest().body("Quantity must be at least 1");
            int invQuantity = inv.getQuantity();
            if (quantity > invQuantity) return ResponseEntity.badRequest().body("Only " + invQuantity + " in stock");
            CartItem ci = new CartItem();
            ci.setCustomerId(customer.getId());
            ci.setInventoryItemId(inv.getId());
            ci.setQuantity(quantity);
            cartItemRepository.save(ci);
        }

        return ResponseEntity.ok("Item added to cart");
    }

    @PostMapping("/removeFromCart")
    public ResponseEntity<String> RemoveFromCart(@RequestParam Long itemId,
                                                @RequestParam int quantity,
                                                HttpSession session) {
        CustomerInfo customer = CustomerInfo.getCustomer(session);
        if (customer == null) return ResponseEntity.status(401).body("Authentication required");

        Optional<InventoryItem> invOpt = inventoryItemRepository.findById(itemId);
        if (invOpt.isEmpty()) return ResponseEntity.badRequest().body("Item not found");

        Optional<CartItem> cartOpt = cartItemRepository.findByCustomerIdAndInventoryItemId(
                customer.getId(), invOpt.get().getId());

        if (cartOpt.isEmpty()) return ResponseEntity.badRequest().body("Item not in cart");

        CartItem ci = cartOpt.get();
        if (ci.getQuantity() > quantity) {
            ci.setQuantity(ci.getQuantity() - quantity);
            cartItemRepository.save(ci);
        } else {
            cartItemRepository.delete(ci);
        }

        return ResponseEntity.ok("Item removed from cart");
    }

    @GetMapping("/getCart")
    public ResponseEntity<?> GetCart(HttpSession session) {
        CustomerInfo customer = CustomerInfo.getCustomer(session);
        if (customer == null) return ResponseEntity.status(401).body("Authentication required");

        List<CartItem> cartItems = cartItemRepository.findByCustomerId(customer.getId());

        // Enrich with current inventory data for the response
        List<Map<String, Object>> result = cartItems.stream().map(ci -> {
            Map<String, Object> entry = new HashMap<>();
            inventoryItemRepository.findById(ci.getInventoryItemId()).ifPresent(inv -> {
                entry.put("id", inv.getId());
                entry.put("name", inv.getName());
                entry.put("price", inv.getPrice());
                entry.put("categoryName", inv.getCategoryName());
            });
            entry.put("quantity", ci.getQuantity());
            return entry;
        }).toList();

        return ResponseEntity.ok(result);
    }

    @PostMapping("/checkout")
    public ResponseEntity<?> Checkout(@RequestParam(required = false) String subscriptionId,
                                    @RequestParam(defaultValue = "false") boolean pickup,
                                    HttpSession session) {
        CustomerInfo customer = CustomerInfo.getCustomer(session);
        if (customer == null) return ResponseEntity.status(401).body("Authentication required");

        try {
            OrderManifest manifest = checkoutService.checkout(customer, subscriptionId, pickup);
            return ResponseEntity.ok(manifest);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

}
