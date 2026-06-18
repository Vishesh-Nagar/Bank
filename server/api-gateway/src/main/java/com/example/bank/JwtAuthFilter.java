package com.example.bank;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class JwtAuthFilter implements GlobalFilter, Ordered {

    private final String jwtSecret;
    private final TokenBlocklistService tokenBlocklistService;
    private final ObjectMapper objectMapper;

    // Paths that never require a JWT
    private static final List<String> PUBLIC_PATHS = List.of(
            "/api/v1/users/login",
            "/ws/info"
    );

    public JwtAuthFilter(@Value("${jwt.secret}") String jwtSecret,
                         TokenBlocklistService tokenBlocklistService,
                         ObjectMapper objectMapper) {
        this.jwtSecret = jwtSecret;
        this.tokenBlocklistService = tokenBlocklistService;
        this.objectMapper = objectMapper;
    }

    private boolean isPublic(ServerHttpRequest request) {
        String path = request.getPath().value();
        HttpMethod method = request.getMethod();
        // Always bypass OPTIONS requests for CORS preflight
        if (HttpMethod.OPTIONS.equals(method)) {
            return true;
        }
        // POST /api/v1/users (registration) is public
        if (HttpMethod.POST.equals(method) && "/api/v1/users".equals(path)) {
            return true;
        }
        return PUBLIC_PATHS.stream().anyMatch(path::equals);
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();

        // Propagate / generate X-Request-Id
        String requestId = request.getHeaders().getFirst("X-Request-Id");
        if (requestId == null || requestId.isBlank()) {
            requestId = UUID.randomUUID().toString();
        }
        final String finalRequestId = requestId;

        if (isPublic(request)) {
            ServerHttpRequest mutated = request.mutate()
                    .header("X-Request-Id", finalRequestId)
                    .build();
            return chain.filter(exchange.mutate().request(mutated).build());
        }

        String tempToken;
        String authHeader = request.getHeaders().getFirst("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            tempToken = authHeader.substring(7);
        } else {
            // Fallback for WebSockets (SockJS)
            tempToken = request.getQueryParams().getFirst("token");
        }
        final String token = tempToken;

        if (token == null || token.isBlank()) {
            return writeError(exchange, HttpStatus.UNAUTHORIZED, "AUTHENTICATION_REQUIRED",
                    "Authentication is required to access this resource.", finalRequestId);
        }

        // Check blocklist first (reactive)
        return tokenBlocklistService.isBlocked(token).flatMap(blocked -> {
            if (blocked) {
                return writeError(exchange, HttpStatus.UNAUTHORIZED, "INVALID_TOKEN",
                        "The provided authentication token is invalid or expired.", finalRequestId);
            }

            try {
                SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes());
                Claims claims = Jwts.parser().verifyWith(key).build()
                        .parseSignedClaims(token).getPayload();

                String username = claims.getSubject();
                Object userIdClaim = claims.get("userId");
                String userId = userIdClaim != null ? userIdClaim.toString() : "";

                ServerHttpRequest mutated = request.mutate()
                        .header("X-Authenticated-User", username)
                        .header("X-Authenticated-UserId", userId)
                        .header("X-Request-Id", finalRequestId)
                        .build();

                return chain.filter(exchange.mutate().request(mutated).build());

            } catch (ExpiredJwtException e) {
                return writeError(exchange, HttpStatus.UNAUTHORIZED, "TOKEN_EXPIRED",
                        "The authentication token has expired. Please log in again.", finalRequestId);
            } catch (SignatureException | MalformedJwtException e) {
                return writeError(exchange, HttpStatus.UNAUTHORIZED, "INVALID_TOKEN",
                        "The provided authentication token is invalid or expired.", finalRequestId);
            } catch (Exception e) {
                return writeError(exchange, HttpStatus.UNAUTHORIZED, "INVALID_TOKEN",
                        "The provided authentication token is invalid or expired.", finalRequestId);
            }
        });
    }

    private Mono<Void> writeError(ServerWebExchange exchange, HttpStatus status,
                                   String code, String message, String requestId) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = Map.of(
                "success", false,
                "error", Map.of("code", code, "message", message),
                "meta", Map.of("requestId", requestId, "timestamp", Instant.now().toString())
        );

        try {
            byte[] bytes = objectMapper.writeValueAsBytes(body);
            DataBuffer buffer = response.bufferFactory().wrap(bytes);
            return response.writeWith(Mono.just(buffer));
        } catch (JsonProcessingException e) {
            return response.setComplete();
        }
    }

    @Override
    public int getOrder() { return -1; }
}
