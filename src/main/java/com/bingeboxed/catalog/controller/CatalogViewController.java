package com.bingeboxed.catalog.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.view.RedirectView;

@Controller
public class CatalogViewController {

    @GetMapping("/")
    public RedirectView home() {
        return new RedirectView("/catalog");
    }

    @GetMapping("/catalog")
    public String browsePage() {
        return "catalog/browse";
    }

    @GetMapping("/catalog/{tmdbId}")
    public String detailPage(@PathVariable Integer tmdbId, @RequestParam String type) {
        return "catalog/detail";
    }
}