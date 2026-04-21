package com.ftf.order;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@Controller
public class UIController {

    private final InventoryItemRepository inventoryItemRepository;
    private final CartItemRepository cartItemRepository;
    private final JwtService jwtService;


    public UIController(InventoryItemRepository inventoryItemRepository, CartItemRepository cartItemRepository, JwtService jwtService) {
        this.inventoryItemRepository = inventoryItemRepository;
        this.cartItemRepository = cartItemRepository;
        this.jwtService = jwtService;
    }

    @GetMapping("/")
    public String index(Model model) {
        return "redirect:/products";
    }

    @GetMapping("/products")
    public String products(Model model) {
        List<InventoryItem> products = inventoryItemRepository.findByActiveTrue();
        model.addAttribute("products", products);
        return "index";
    }

    @GetMapping("/cart")
    public String cart(Model model, HttpSession session, HttpServletRequest request) {
        CustomerInfo customer = CustomerInfo.getCustomer(session, request, jwtService);
        if (customer == null) return null;

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

    @GetMapping("/cartdebug")
    public String cartdebug(Model model) {

        List<CartItem> cartItems = cartItemRepository.findByCustomerId("123456789");
        System.out.println("sise"+cartItems.size());

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
