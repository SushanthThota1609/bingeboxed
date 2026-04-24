package com.bingeboxed.social.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class PublicProfileViewController {

    @GetMapping("/profile/public/{userId}")
    public String publicProfile(@PathVariable Long userId) {
        return "profiles/public";
    }
}
