package com.bingeboxed.social.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/users")
public class UserViewController {
    
    @GetMapping("/{userId}/profile")
    public String userProfile(@PathVariable Long userId, Model model) {
        model.addAttribute("userId", userId);
        return "users/profile";
    }
}