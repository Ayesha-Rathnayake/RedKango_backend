package com.example.backend.web;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/customer")
public class CustomController {

    @GetMapping
    @PreAuthorize("hasAnyRole('CUSTOMER','ADMIN')")
    public Map<String, Object> profile(Authentication auth) {
        return Map.of("email", auth.getName(), "message", "Welcome, customer!");
    }
}