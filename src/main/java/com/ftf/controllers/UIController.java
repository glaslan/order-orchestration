package com.ftf.controllers;


import java.util.ArrayList;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.http.ResponseEntity;

import com.ftf.order.Category;
import com.ftf.order.Item;

@Controller
public class UIController {

    @GetMapping("/")
    public String index(Model model) {
        // redirect to the products page 
        return "redirect:/products";
    }

    @GetMapping("/products")
    public String products(Model model) {
        ArrayList<Item> products = new ArrayList<Item>();

        // TODO
        // need to query database and gett all the items to pass to the UI

        // TODO
        // this is temp for now while waiting for inventory team 
        products.add(new Item("Apple", 1.99, 0, 1, new Category("Produce", 0)));
        products.add(new Item("Banana", 0.99, 0, 2, new Category("Produce", 0)));
        products.add(new Item("Orange", 2.99, 0, 3, new Category("Produce", 0)));
        
        model.addAttribute("products", products);

        return "index";
    }
    
}
