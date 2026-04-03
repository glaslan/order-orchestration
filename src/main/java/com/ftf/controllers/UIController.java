package com.ftf.controllers;


import java.util.ArrayList;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.ftf.order.Item;

@Controller
public class UIController {

    @GetMapping("/")
    public String index(Model model) {
        ArrayList<Item> products = new ArrayList<Item>();

        // TODO
        // need to query database and gett all the items to pass to the UI

        // TODO
        // this is temp for now while waiting for inventory team 
        products.add(new Item("Apple", 1.99, 0, 1));
        products.add(new Item("Banana", 0.99, 0, 2));
        products.add(new Item("Orange", 2.99, 0, 3));
        
        model.addAttribute("products", products);

        return "index";
    }
    
}
