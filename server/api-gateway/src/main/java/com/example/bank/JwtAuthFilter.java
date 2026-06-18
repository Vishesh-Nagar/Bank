package com.example.bank;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.Ordered;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.util.List;

@Component
public class JwtAuthFilter implements GlobalFilter, Ordered {

    @Value("${jwt.secret:mysecretkey1234567890123456789012}")
    private String jwtSecret;

    // Paths that don't require a JWT token
    private static final List<String> PUBLIC_PATHS = List.of(
            "/api/users/login"
    );

    // Paths that are public only for specific methods
    private boolean isPublic(ServerHttpRequest request) {
        String path = request.getPath().value();
        HttpMethod method = request.getMethod();

        // POST /api/users (registration) is public
        if (HttpMethod.POST.equals(method) && "/api/users".equals(path)) {
            return true;
        }

        // Exact public paths (any method)
        return PUBLIC_PATHS.stream().anyMatch(path::equals);
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        if (isPublic(exchange.getRequest())) {
            return chain.filter(exchange);
        }

        String authHeader = exchange.getRequest().getHeaders().getFirst("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        try {
            SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes());
            Claims claims = Jwts.parser().verifyWith(key).build()
                    .parseSignedClaims(authHeader.substring(7)).getPayload();

            ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                    .header("X-Authenticated-User", claims.getSubject())
                    .build();

            return chain.filter(exchange.mutate().request(mutatedRequest).build());

        } catch (Exception e) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }
    }

    @Override
    public int getOrder() { return -1; }
}
