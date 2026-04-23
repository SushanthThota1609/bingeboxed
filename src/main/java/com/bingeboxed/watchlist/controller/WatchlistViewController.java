package com.bingeboxed.watchlist.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class WatchlistViewController {

    @GetMapping("/watchlist")
    public String watchlistPage(Model model) {
        model.addAttribute("activePage", "watchlist");
        return "watchlist/index";
    }

    @GetMapping("/watchlist/user/{userId}")
    public String publicWatchlistPage(@PathVariable Long userId, Model model) {
        model.addAttribute("userId", userId);
        return "watchlist/public";
    }
}
