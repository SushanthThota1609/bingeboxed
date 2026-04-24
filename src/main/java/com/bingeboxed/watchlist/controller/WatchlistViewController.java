// src/main/java/com/bingeboxed/watchlist/controller/WatchlistViewController.java
package com.bingeboxed.watchlist.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class WatchlistViewController {

    @GetMapping("/watchlist")
    public String myWatchlist() {
        return "watchlist/index";
    }

    @GetMapping("/watchlist/user/{userId}")
    public String userWatchlist(@PathVariable Long userId, Model model) {
        model.addAttribute("userId", userId);
        return "watchlist/user";
    }
}