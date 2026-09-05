package com.example.gateway.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class JwtAuthFilterTest {

    private static final String SECRET = "test-secret-key-at-least-32-bytes-long!!";

    private final JwtAuthFilter filter = new JwtAuthFilter(SECRET);

    @Test
    void missingAuthorizationHeader_returns401AndDoesNotCallChain() {
        ServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/order/1"));
        AtomicBoolean chainCalled = new AtomicBoolean(false);

        filter.filter(exchange, chainStub(chainCalled, null)).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(chainCalled).isFalse();
    }

    @Test
    void invalidToken_returns401AndDoesNotCallChain() {
        ServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/order/1")
                        .header("Authorization", "Bearer garbage-not-a-jwt"));
        AtomicBoolean chainCalled = new AtomicBoolean(false);

        filter.filter(exchange, chainStub(chainCalled, null)).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(chainCalled).isFalse();
    }

    @Test
    void validToken_forwardsUserHeadersAndCallsChain() {
        UUID userId = UUID.randomUUID();
        String token = generateToken(userId, "ADMIN");

        ServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/order/1")
                        .header("Authorization", "Bearer " + token));
        AtomicBoolean chainCalled = new AtomicBoolean(false);
        AtomicReference<ServerHttpRequest> capturedRequest = new AtomicReference<>();

        filter.filter(exchange, chainStub(chainCalled, capturedRequest)).block();

        assertThat(chainCalled).isTrue();
        assertThat(capturedRequest.get().getHeaders().getFirst("X-User-Id")).isEqualTo(userId.toString());
        assertThat(capturedRequest.get().getHeaders().getFirst("X-User-Role")).isEqualTo("ADMIN");
    }

    @Test
    void publicPath_bypassesAuthEvenWithoutToken() {
        ServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/auth/login"));
        AtomicBoolean chainCalled = new AtomicBoolean(false);

        filter.filter(exchange, chainStub(chainCalled, null)).block();

        assertThat(chainCalled).isTrue();
    }

    private GatewayFilterChain chainStub(AtomicBoolean chainCalled, AtomicReference<ServerHttpRequest> capturedRequest) {
        return ex -> {
            chainCalled.set(true);
            if (capturedRequest != null) {
                capturedRequest.set(ex.getRequest());
            }
            return Mono.empty();
        };
    }

    private String generateToken(UUID userId, String role) {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        Date now = new Date();
        return Jwts.builder()
                .subject(userId.toString())
                .claim("role", role)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + 3_600_000))
                .signWith(key)
                .compact();
    }
}
