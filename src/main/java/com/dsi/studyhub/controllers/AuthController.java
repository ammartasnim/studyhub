package com.dsi.studyhub.controllers;

import com.dsi.studyhub.dtos.AuthResDto;
import com.dsi.studyhub.dtos.LoginReqDto;
import com.dsi.studyhub.dtos.RegisterReqDto;
import com.dsi.studyhub.entities.User;
import com.dsi.studyhub.security.JwtService;
import com.dsi.studyhub.services.AuthService;
import io.jsonwebtoken.Claims;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @Autowired
    private JwtService jwtService;

    @PostMapping("/register")
    public ResponseEntity<AuthResDto> register(@Valid @RequestBody RegisterReqDto request) {
        return ResponseEntity.ok(authService.registerUser(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResDto> login(@Valid @RequestBody LoginReqDto request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @GetMapping("/sync")
    public ResponseEntity<AuthResDto> syncSocialUser(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(401).body(null);
        }

        String token = authHeader.substring(7);

        try {
            Claims claims = jwtService.extractAllClaims(token);

            if (claims == null || !jwtService.isSupabaseToken(claims)) {
                return ResponseEntity.status(401).body(null);
            }

            String uid = claims.getSubject();
            String email = claims.get("email", String.class);

            // Extract given_name and family_name from user_metadata (Google provides these)
            String firstName = null;
            String lastName = null;

            Object rawMeta = claims.get("user_metadata");
            if (rawMeta instanceof Map<?, ?> meta) {
                firstName = (String) meta.get("given_name");
                lastName  = (String) meta.get("family_name");

                // fallback: if Google only gave "name" (unlikely but safe)
                if (firstName == null) {
                    String fullName = (String) meta.get("name");
                    if (fullName != null && !fullName.isBlank()) {
                        String[] parts = fullName.trim().split(" ", 2);
                        firstName = parts[0];
                        lastName  = parts.length > 1 ? parts[1] : "";
                    }
                }
            }

            UserDetails userDetails = authService.syncAndReturnUserDetails(uid, email, firstName, lastName);
            User user = (User) userDetails;
            String localToken = jwtService.generateToken(userDetails);

            return ResponseEntity.ok(new AuthResDto(
                    localToken,
                    user.getId(),
                    user.getUsername(),
                    user.getRole()
            ));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(401).body(null);
        }
    }
}
