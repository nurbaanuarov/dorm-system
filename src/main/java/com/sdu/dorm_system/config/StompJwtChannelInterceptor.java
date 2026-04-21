package com.sdu.dorm_system.config;

import com.sdu.dorm_system.domain.UserAccount;
import com.sdu.dorm_system.domain.enums.Role;
import com.sdu.dorm_system.exception.BusinessException;
import com.sdu.dorm_system.repository.UserAccountRepository;
import com.sdu.dorm_system.service.JwtTokenService;
import java.security.Principal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class StompJwtChannelInterceptor implements ChannelInterceptor {

    private final JwtTokenService jwtTokenService;
    private final UserAccountRepository userAccountRepository;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() == null) {
            return message;
        }

        return switch (accessor.getCommand()) {
            case CONNECT -> authenticate(accessor, message);
            case SUBSCRIBE, SEND -> ensureStudentUser(accessor, message);
            default -> message;
        };
    }

    private Message<?> authenticate(StompHeaderAccessor accessor, Message<?> message) {
        String authorizationHeader = accessor.getFirstNativeHeader("Authorization");
        if (!StringUtils.hasText(authorizationHeader) || !authorizationHeader.startsWith("Bearer ")) {
            throw BusinessException.unauthorized("JWT token is required for WebSocket connection");
        }

        String token = authorizationHeader.substring(7).trim();
        JwtTokenService.JwtClaims claims = jwtTokenService.parseAndValidate(token);
        UserAccount user = userAccountRepository.findByEmailIgnoreCase(claims.subject())
            .orElseThrow(() -> BusinessException.unauthorized("Authenticated user was not found"));

        if (!user.isEnabled()) {
            throw BusinessException.unauthorized("User account is disabled");
        }

        if (user.getRole() != Role.STUDENT) {
            throw BusinessException.forbidden("Only students can connect to the global chat");
        }

        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
            user.getEmail(),
            null,
            List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
        );
        accessor.setUser(authentication);
        return message;
    }

    private Message<?> ensureStudentUser(StompHeaderAccessor accessor, Message<?> message) {
        Principal principal = accessor.getUser();
        if (!(principal instanceof UsernamePasswordAuthenticationToken authentication)) {
            throw BusinessException.unauthorized("WebSocket authentication is required");
        }

        boolean studentRole = authentication.getAuthorities().stream()
            .anyMatch(authority -> "ROLE_STUDENT".equals(authority.getAuthority()));

        if (!studentRole) {
            throw BusinessException.forbidden("Only students can use the global chat");
        }

        return message;
    }
}
