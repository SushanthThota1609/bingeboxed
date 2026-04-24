package com.bingeboxed.social.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SocialViewController {

    @GetMapping("/social")
    public String socialPage() {
        return "social/index";
    }
}
