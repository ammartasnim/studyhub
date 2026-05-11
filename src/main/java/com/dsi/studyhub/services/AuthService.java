package com.dsi.studyhub.services;

import com.dsi.studyhub.dtos.AuthResDto;
import com.dsi.studyhub.dtos.LoginReqDto;
import com.dsi.studyhub.dtos.RegisterReqDto;
import org.springframework.security.core.userdetails.UserDetails;

public interface AuthService {
    AuthResDto registerUser(RegisterReqDto request);
    AuthResDto login(LoginReqDto request);
    UserDetails syncAndReturnUserDetails(String uid, String email, String firstName, String lastName);
}
