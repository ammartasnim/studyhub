package com.dsi.studyhub.security;

import com.dsi.studyhub.services.AuthService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    @Autowired
    private JwtService jwtService;

    @Autowired
    private CustomUserDetailsService userDetailsService;
    @Autowired
    @Lazy
    private AuthService authService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        // Resolves JWT from Authorization header and builds the SecurityContext.
        final String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        final String token = authHeader.substring(7);

        try {
            // Claims parsing
            Claims claims = jwtService.extractAllClaims(token);

            if (claims != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails;

                // Token source
                if (jwtService.isSupabaseToken(claims)) {
                    // Supabase sync
                    String uid = claims.getSubject();
                    String email = claims.get("email", String.class);
                    String firstName = null;
                    String lastName = null;

                    Object rawMeta = claims.get("user_metadata");
                    if (rawMeta instanceof Map<?, ?> meta) {
                        firstName = (String) meta.get("given_name");
                        lastName  = (String) meta.get("family_name");
                        if (firstName == null) {
                            String fullName = (String) meta.get("name");
                            if (fullName != null && !fullName.isBlank()) {
                                String[] parts = fullName.trim().split(" ", 2);
                                firstName = parts[0];
                                lastName  = parts.length > 1 ? parts[1] : "";
                            }
                        }
                    }

                    userDetails = authService.syncAndReturnUserDetails(uid, email, firstName, lastName);
                } else {
                    // Local user lookup
                    String username = claims.getSubject();
                    userDetails = userDetailsService.loadUserByUsername(username);
                }


                boolean isValid = jwtService.isSupabaseToken(claims) ||
                        jwtService.isTokenValid(token, userDetails);

                if (isValid) {
                    if (!userDetails.isAccountNonLocked()) {
                        filterChain.doFilter(request, response);
                        return;
                    }
                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails, null, userDetails.getAuthorities()
                            );
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (Exception e) {
            logger.warn("JWT authentication failed: " + e.getMessage());
        }
        filterChain.doFilter(request, response);
    }
}
