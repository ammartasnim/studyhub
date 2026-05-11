package com.dsi.studyhub.websocket;

import com.dsi.studyhub.entities.User;
import com.dsi.studyhub.repositories.UserRepository;
import com.dsi.studyhub.security.JwtService;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class WebSocketAuthChannelInterceptor implements ChannelInterceptor {
    private static final String SESSION_AUTH_KEY = "WS_AUTH";

    private final JwtService jwtService;
    private final UserRepository userRepository;

    public WebSocketAuthChannelInterceptor(JwtService jwtService, UserRepository userRepository) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        // Resolves a WebSocket user from session or Authorization header.
        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor == null) return message;

        Authentication authentication = null;
        Map<String, Object> sessionAttributes = accessor.getSessionAttributes();

        // Authentication resolution
        if (accessor.getUser() instanceof Authentication existing) {
            authentication = existing;
        }

        if (authentication == null && sessionAttributes != null) {
            if (sessionAttributes.get(SESSION_AUTH_KEY) instanceof Authentication stored) {
                authentication = stored;
                accessor.setUser(authentication);
            }
        }

        if (authentication == null) {
            String token = extractBearer(accessor.getNativeHeader("Authorization"));
            if (token == null) {
                token = extractBearer(accessor.getNativeHeader("authorization"));
            }
            if (token != null) {
                String username = jwtService.extractUsername(token);
                User user = userRepository.findByUsername(username).orElse(null);
                if (user != null) {
                    if (!user.isAccountNonLocked()) {
                        return message;
                    }
                    authentication = new UsernamePasswordAuthenticationToken(
                            user, null, user.getAuthorities());
                    accessor.setUser(authentication);
                    if (sessionAttributes != null) {
                        sessionAttributes.put(SESSION_AUTH_KEY, authentication);
                    }
                }
            }
        }

        // Thread context binding
        if (authentication != null) {
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        return message;
    }

    private String extractBearer(List<String> values) {
        if (values == null || values.isEmpty()) return null;
        String header = values.get(0);
        if (header == null) return null;
        return header.startsWith("Bearer ") ? header.substring(7) : header;
    }
}
