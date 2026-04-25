package com.bingeboxed.recommendations.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class RecommendationViewController {

    @GetMapping("/recommendations")
    public String recommendationsPage() {
        return "recommendations/index";
    }
}
