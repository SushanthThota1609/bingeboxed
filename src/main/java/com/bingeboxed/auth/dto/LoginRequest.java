// src/main/java/com/bingeboxed/auth/dto/LoginRequest.java
package com.bingeboxed.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank @Email String email,
        @NotBlank String password
) {}