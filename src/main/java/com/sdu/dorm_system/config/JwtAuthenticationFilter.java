package com.sdu.dorm_system.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sdu.dorm_system.domain.UserAccount;
import com.sdu.dorm_system.repository.UserAccountRepository;
import com.sdu.dorm_system.service.JwtTokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenService jwtTokenService;
    private final UserAccountRepository userAccountRepository;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {
        String authorizationHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (!StringUtils.hasText(authorizationHeader) || !authorizationHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authorizationHeader.substring(7).trim();
        if (!StringUtils.hasText(token)) {
            writeUnauthorized(response, request.getRequestURI(), "JWT token is missing");
            return;
        }

        UserAccount user;
        try {
            JwtTokenService.JwtClaims claims = jwtTokenService.parseAndValidate(token);
            user = userAccountRepository.findByEmailIgnoreCase(claims.subject())
                .orElseThrow(() -> new IllegalArgumentException("Authenticated user was not found"));
        } catch (Exception exception) {
            SecurityContextHolder.clearContext();
            writeUnauthorized(response, request.getRequestURI(), "Invalid or expired token");
            return;
        }

        if (!user.isEnabled()) {
            writeUnauthorized(response, request.getRequestURI(), "User account is disabled");
            return;
        }

        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                user.getEmail(),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
            );
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(request, response);
    }

    private void writeUnauthorized(HttpServletResponse response, String path, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), Map.of(
            "status", HttpServletResponse.SC_UNAUTHORIZED,
            "message", message,
            "path", path,
            "details", List.of(),
            "timestamp", OffsetDateTime.now()
        ));
    }
}
