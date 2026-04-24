package com.ftf.order.controller;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.ftf.order.model.CartItem;
import com.ftf.order.model.CartItemWithInventory;
import com.ftf.order.model.CustomerInfo;
import com.ftf.order.model.InventoryItem;
import com.ftf.order.repository.CartItemRepository;
import com.ftf.order.repository.InventoryItemRepository;
import jakarta.servlet.http.HttpSession;

@Controller
public class UIController {

    private final InventoryItemRepository inventoryItemRepository;
    private final CartItemRepository cartItemRepository;

    public UIController(InventoryItemRepository inventoryItemRepository, CartItemRepository cartItemRepository) {
        this.inventoryItemRepository = inventoryItemRepository;
        this.cartItemRepository = cartItemRepository;
    }

    @GetMapping("/")
    public String index(Model model) {
        return "redirect:/products";
    }

    @GetMapping("/products")
    public String products(Model model, Httpsession session, CustomerInfo customer) {

        List<InventoryItem> products = inventoryItemRepository.findByActiveTrue();
        model.addAttribute("products", products);
            CustomerInfo customer = jwtService.parse(token);
            session.invalidate();
            HttpSession fresh = request.getSession(true);
            fresh.setAttribute("customer", customer);
            return "index";
        

    }

    @GetMapping("/cart")
    public String cart(Model model, HttpSession session) {
        CustomerInfo customer = CustomerInfo.getCustomer(session);
        if (customer == null) return "redirect:/products";

        List<CartItem> cartItems = cartItemRepository.findByCustomerId(customer.getId());

        // Create a list of objects containing both cart item and inventory item data
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
