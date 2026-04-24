// src/main/java/com/bingeboxed/reviews/controller/ReviewsViewController.java
package com.bingeboxed.reviews.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ReviewsViewController {
    
    @GetMapping("/reviews")
    public String reviewsIndex(Model model) {
        // Just return the template - authentication handled by SecurityConfig
        return "reviews/index";
    }
}