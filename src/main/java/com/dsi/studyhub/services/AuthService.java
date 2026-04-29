package com.dsi.studyhub.services;

import com.dsi.studyhub.dtos.AuthResDto;
import com.dsi.studyhub.dtos.LoginReqDto;
import com.dsi.studyhub.dtos.RegisterReqDto;
import com.dsi.studyhub.entities.Badge;
import com.dsi.studyhub.entities.User;
import com.dsi.studyhub.enums.BadgeType;
import com.dsi.studyhub.enums.UserRole;
import com.dsi.studyhub.exceptions.ConflictException;
import com.dsi.studyhub.repositories.UserRepository;
import com.dsi.studyhub.security.JwtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private AuthenticationManager authenticationManager;


    public AuthResDto registerUser(RegisterReqDto request) {

        if (userRepository.existsByEmail(request.email()))
            throw new ConflictException("Email already in use");

        if (userRepository.existsByUsername(request.username()))
            throw new ConflictException("Username already taken");

        User user = new User();
        user.setUsername(request.username());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setEmail(request.email());
        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user.setPhone(request.phone());
        user.setPfp(request.pfp());
        user.setRole(UserRole.Client);
        user.setBanned(false);
        user.setXpPts(0);
        user.setLevel(1);

        Badge beginnerBadge = new Badge();
        beginnerBadge.setUser(user);
        beginnerBadge.setType(BadgeType.BEGINNER);

        user.getBadges().add(beginnerBadge);

        User savedUser = userRepository.save(user);

        String token = jwtService.generateToken(user);
        return new AuthResDto(token, savedUser.getId(), savedUser.getUsername(), savedUser.getRole());
    }

    public AuthResDto login(LoginReqDto request) {
        String input = request.username();

        boolean looksLikeEmail = input.contains("@");

        var user = looksLikeEmail
                ? userRepository.findByEmail(input)
                        .orElseThrow(() -> new UsernameNotFoundException("User not found"))
                : userRepository.findByUsername(input)
                        .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        user.getUsername(),
                        request.password()
                )
        );

        String token = jwtService.generateToken(user);
        return new AuthResDto(token, user.getId(), user.getUsername(), user.getRole());
    }
}
