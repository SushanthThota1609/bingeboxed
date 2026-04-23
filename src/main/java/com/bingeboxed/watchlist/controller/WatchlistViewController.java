package com.bingeboxed.watchlist.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class WatchlistViewController {

    @GetMapping("/watchlist")
    public String watchlistPage() {
        return "watchlist/index";
    }
}