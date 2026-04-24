package com.ftf.order.controller;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.ftf.order.model.CartItem;
import com.ftf.order.model.CartItemWithInventory;
import com.ftf.order.model.CustomerInfo;
import com.ftf.order.model.InventoryItem;
import com.ftf.order.repository.CartItemRepository;
import com.ftf.order.repository.InventoryItemRepository;
import com.ftf.order.service.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@Controller
public class UIController {

    private static final Logger log = LoggerFactory.getLogger(UIController.class);

    private final InventoryItemRepository inventoryItemRepository;
    private final CartItemRepository cartItemRepository;
    private final JwtService jwtService;
    private final String customerLoginUrl;

    public UIController(InventoryItemRepository inventoryItemRepository,
                        CartItemRepository cartItemRepository,
                        JwtService jwtService,
                        @Value("${teams.customer.login-url}") String customerLoginUrl) {
        this.inventoryItemRepository = inventoryItemRepository;
        this.cartItemRepository = cartItemRepository;
        this.jwtService = jwtService;
        this.customerLoginUrl = customerLoginUrl;
    }

    @GetMapping("/")
    public String index() {
        return "redirect:/products";
    }

    @GetMapping("/products")
    public String products(@RequestParam(value = "token", required = false) String token,
                           Model model,
                           HttpSession session,
                           HttpServletRequest request) {
        // Customer team redirects here with ?token=<jwt> after login.
        // Consume the token once, stash the customer on a fresh session, then reload
        // without the token in the URL (keeps it out of browser history / referer logs).
        if (token != null && !token.isBlank()) {
            log.info("Received token handoff, length={} prefix={}",
                    token.length(), token.substring(0, Math.min(20, token.length())));
            try {
                CustomerInfo customer = jwtService.parse(token);
                log.info("Token parsed successfully for customer id={} email={}",
                        customer.getId(), customer.getEmail());
                session.invalidate();
                HttpSession fresh = request.getSession(true);
                fresh.setAttribute("customer", customer);
                return "redirect:/products";
            } catch (Exception e) {
                log.warn("Token parse failed: {} ({})", e.getMessage(), e.getClass().getSimpleName());
                return "redirect:" + customerLoginUrl;
            }
        }

        if (CustomerInfo.getCustomer(session) == null) {
            log.info("No session customer on /products, redirecting to login");
            return "redirect:" + customerLoginUrl;
        }

        List<InventoryItem> products = inventoryItemRepository.findByActiveTrue();
        model.addAttribute("products", products);
        return "index";
    }

    @GetMapping("/cart")
    public String cart(Model model, HttpSession session) {
        CustomerInfo customer = CustomerInfo.getCustomer(session);
        if (customer == null) return "redirect:" + customerLoginUrl;

        List<CartItem> cartItems = cartItemRepository.findByCustomerId(customer.getId());

        List<CartItemWithInventory> cartItemsWithInventory = cartItems.stream()
                .map(cartItem -> {
                    InventoryItem inventoryItem = inventoryItemRepository.findById(cartItem.getInventoryItemId())
                            .orElse(null);
                    return new CartItemWithInventory(cartItem, inventoryItem);
                })
                .toList();

        model.addAttribute("items", cartItemsWithInventory);
        return "cart";
    }

}
