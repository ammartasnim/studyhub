package com.dsi.studyhub.dtos;

import com.dsi.studyhub.enums.UserRole;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class AuthResponse {
    private String token;
    private String userId;
    private String username;
    private UserRole role;
}