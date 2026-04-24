// src/main/java/com/bingeboxed/reviews/controller/ReviewsViewController.java
package com.bingeboxed.reviews.controller;

import com.bingeboxed.shared.security.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.server.ResponseStatusException;

@Controller
public class ReviewsViewController {

    private final JwtService jwtService;

    public ReviewsViewController(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    // FR-11: requires authenticated user
   @GetMapping("/reviews")
    public String reviewsIndex() {
        return "reviews/index";
    }
}