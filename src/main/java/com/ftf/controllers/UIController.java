package com.ftf.controllers;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.ftf.order.InventoryItem;
import com.ftf.order.InventoryItemRepository;

@Controller
public class UIController {

    private final InventoryItemRepository inventoryItemRepository;

    public UIController(InventoryItemRepository inventoryItemRepository) {
        this.inventoryItemRepository = inventoryItemRepository;
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

    @GetMapping("/cartdebug")
    public String cartdebug() {
        return "cart";
    }
}
