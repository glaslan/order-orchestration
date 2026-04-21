package com.ftf.order;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.ftf.order.CartItem;
import com.ftf.order.CartItemRepository;
import com.ftf.order.CheckoutService;
import com.ftf.order.CustomerInfo;
import com.ftf.order.InventoryItem;
import com.ftf.order.InventoryItemRepository;
import com.ftf.order.JwtAuthFilter;
import com.ftf.order.JwtService;
import com.ftf.order.OrderManifest;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

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

    // Stores the customer's validated JWT in their session so web UI forms work without
    // manually adding an Authorization header on every request.
    @PostMapping("/auth/session")
    public ResponseEntity<String> setSession(@org.springframework.web.bind.annotation.RequestBody String token,
                                             HttpSession session) {
        try {
            String rawToken = token.trim().replace("\"", "");
            CustomerInfo customer = jwtService.parse(rawToken);
            session.setAttribute("customer", customer);
            session.setAttribute("jwt_raw", rawToken);
            return ResponseEntity.ok("Session established for " + customer.getName());
        } catch (Exception e) {
            return ResponseEntity.status(401).body("Invalid token: " + e.getMessage());
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
                                            HttpSession session,
                                            HttpServletRequest request) {
        CustomerInfo customer = getCustomer(request);
        if (customer == null) return ResponseEntity.status(401).body("Authentication required");

        // Look up by sourceItemId — that's what the UI sends via the hidden input
        Optional<InventoryItem> invOpt = inventoryItemRepository.findBySourceItemId(itemId);
        if (invOpt.isEmpty() || !invOpt.get().isActive()) {
            return ResponseEntity.badRequest().body("Item not available");
        }
        InventoryItem inv = invOpt.get();

        Optional<CartItem> existing = cartItemRepository.findByCustomerIdAndInventoryItemId(
                customer.getId(), inv.getId());

        if (existing.isPresent()) {
            CartItem ci = existing.get();
            ci.setQuantity(ci.getQuantity() + quantity);
            cartItemRepository.save(ci);
        } else {
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
                                                 HttpSession session,
                                                 HttpServletRequest request) {
        CustomerInfo customer = getCustomer(request);
        if (customer == null) return ResponseEntity.status(401).body("Authentication required");

        Optional<InventoryItem> invOpt = inventoryItemRepository.findBySourceItemId(itemId);
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
    public ResponseEntity<?> GetCart(HttpSession session, HttpServletRequest request) {
        CustomerInfo customer = getCustomer(request);
        if (customer == null) return ResponseEntity.status(401).body("Authentication required");

        List<CartItem> cartItems = cartItemRepository.findByCustomerId(customer.getId());

        // Enrich with current inventory data for the response
        List<Map<String, Object>> result = cartItems.stream().map(ci -> {
            Map<String, Object> entry = new HashMap<>();
            inventoryItemRepository.findById(ci.getInventoryItemId()).ifPresent(inv -> {
                entry.put("sourceItemId", inv.getSourceItemId());
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
                                      HttpSession session,
                                      HttpServletRequest request) {
        CustomerInfo customer = getCustomer(request);
        if (customer == null) return ResponseEntity.status(401).body("Authentication required");

        // Use header token if present; fall back to the raw token stored at session login.
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken == null) bearerToken = "Bearer " + session.getAttribute("jwt_raw");

        try {
            OrderManifest manifest = checkoutService.checkout(customer, subscriptionId, bearerToken, pickup);
            return ResponseEntity.ok(manifest);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // Reads the CustomerInfo resolved by JwtAuthFilter. Falls back to direct header
    // parsing for safety during development when the filter may not be active.
    private CustomerInfo getCustomer(HttpServletRequest request) {
        CustomerInfo fromFilter = (CustomerInfo) request.getAttribute(JwtAuthFilter.CUSTOMER_ATTR);
        if (fromFilter != null) return fromFilter;
        return jwtService.extractFromHeader(request.getHeader("Authorization"));
    }
}
