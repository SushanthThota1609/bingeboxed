// src/main/java/com/bingeboxed/profiles/controller/ProfileViewController.java
package com.bingeboxed.profiles.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ProfileViewController {

    @GetMapping("/profile")
    public String viewProfile() {
        return "profiles/view";
    }

    @GetMapping("/profile/edit")
    public String editProfile() {
        return "profiles/edit";
    }
}