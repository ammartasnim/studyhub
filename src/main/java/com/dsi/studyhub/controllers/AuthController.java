package com.dsi.studyhub.controllers;

import com.dsi.studyhub.dtos.AuthResponse;
import com.dsi.studyhub.dtos.LoginRequest;
import com.dsi.studyhub.dtos.RegisterRequest;
import com.dsi.studyhub.services.AuthService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    // POST /api/auth/register → registerClient() from your diagram
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.registerClient(request));
    }

    // POST /api/auth/login → login() from your diagram
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }
}