// src/main/java/com/bingeboxed/watchlist/controller/WatchlistViewController.java
package com.bingeboxed.watchlist.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * FR-09: Serves the Thymeleaf watchlist index page.
 * Authentication is enforced client-side (redirect to /login if no token).
 * Spring Security must permit GET /watchlist for authenticated users.
 */
@Controller
public class WatchlistViewController {

    @GetMapping("/watchlist")
    public String watchlistPage(Model model) {
        model.addAttribute("activePage", "watchlist");
        return "watchlist/index";
    }
}