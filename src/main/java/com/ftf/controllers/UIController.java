package com.ftf.controllers;


import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class UIController {

    @GetMapping("/")
    public String index() {
        // I believe this returns the index page 
        return "index";
    }
    
}
