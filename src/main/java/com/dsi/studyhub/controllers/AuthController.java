package com.dsi.studyhub.controllers;

import com.dsi.studyhub.dtos.AuthResDto;
import com.dsi.studyhub.dtos.LoginReqDto;
import com.dsi.studyhub.dtos.RegisterReqDto;
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

    @PostMapping("/register")
    public ResponseEntity<AuthResDto> register(@Valid @RequestBody RegisterReqDto request) {
        return ResponseEntity.ok(authService.registerUser(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResDto> login(@Valid @RequestBody LoginReqDto request) {
        return ResponseEntity.ok(authService.login(request));
    }
}
