package com.dsi.studyhub.services;

import com.dsi.studyhub.dtos.AuthResponse;
import com.dsi.studyhub.dtos.LoginRequest;
import com.dsi.studyhub.dtos.RegisterRequest;
import com.dsi.studyhub.entities.Client;
import com.dsi.studyhub.enums.UserRole;
import com.dsi.studyhub.repositories.ClientRepository;
import com.dsi.studyhub.repositories.UserRepository;
import com.dsi.studyhub.security.JwtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class AuthService {

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private AuthenticationManager authenticationManager;


    public AuthResponse registerClient(RegisterRequest request) {

        if (clientRepository.existsByEmail(request.getEmail()))
            throw new RuntimeException("Email already in use");

        if (clientRepository.existsByUsername(request.getUsername()))
            throw new RuntimeException("Username already taken");

        Client client = new Client();
        client.setUserId(UUID.randomUUID().toString());
        client.setUsername(request.getUsername());
        client.setPassword(passwordEncoder.encode(request.getPassword()));
        client.setEmail(request.getEmail());
        client.setFirstName(request.getFirstName());
        client.setLastName(request.getLastName());
        client.setPhone(request.getPhone());
        client.setPfp(request.getPfp());
        client.setRole(UserRole.Client);
        client.setBanned(false);

        clientRepository.save(client);

        String token = jwtService.generateToken(client);
        return new AuthResponse(token, client.getUserId(), client.getUsername(), client.getRole());
    }

    public AuthResponse login(LoginRequest request) {

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(),
                        request.getPassword()
                )
        );

        var user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        String token = jwtService.generateToken(user);
        return new AuthResponse(token, user.getUserId(), user.getUsername(), user.getRole());
    }
}