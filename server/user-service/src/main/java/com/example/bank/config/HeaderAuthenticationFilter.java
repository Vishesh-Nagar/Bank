package com.example.bank.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class HeaderAuthenticationFilter extends OncePerRequestFilter {

    private final String internalServiceSecret;

    public HeaderAuthenticationFilter(@Value("${internal.service-secret}") String internalServiceSecret) {
        this.internalServiceSecret = internalServiceSecret;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();

        // Internal paths — validate X-Internal-Auth header
        if (path.startsWith("/internal/")) {
            String secret = request.getHeader("X-Internal-Auth");
            if (internalServiceSecret == null || !internalServiceSecret.equals(secret)) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("{\"success\":false,\"error\":{\"code\":\"AUTHENTICATION_REQUIRED\",\"message\":\"Internal authentication required.\"}}");
                return;
            }
            // Allow internal calls through with a synthetic INTERNAL_SERVICE authority
            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                    "internal-service", null,
                    List.of(new SimpleGrantedAuthority("ROLE_INTERNAL_SERVICE")));
            SecurityContextHolder.getContext().setAuthentication(auth);
            filterChain.doFilter(request, response);
            return;
        }

        // Public paths — set authentication from X-Authenticated-User (set by gateway)
        String username = request.getHeader("X-Authenticated-User");
        String userIdHeader = request.getHeader("X-Authenticated-UserId");

        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(username, userIdHeader, authorities);
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(request, response);
    }
}
