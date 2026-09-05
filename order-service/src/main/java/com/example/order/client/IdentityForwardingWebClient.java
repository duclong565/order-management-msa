package com.example.order.client;

import com.example.order.security.CurrentUserProvider;
import com.example.order.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

// Mọi service khác (product-service, auth-service) tự bảo vệ endpoint bằng HeaderAuthFilter,
// nên gọi service-to-service vẫn phải mang theo danh tính người dùng hiện tại - forward lại
// đúng header gateway đã gắn cho request gốc, lấy từ CurrentUserProvider.
@Component
@RequiredArgsConstructor
public class IdentityForwardingWebClient {

    private final WebClient.Builder webClientBuilder;
    private final CurrentUserProvider currentUserProvider;

    public WebClient client() {
        WebClient base = webClientBuilder.build();
        return currentUserProvider.findPrincipal()
                .map(this::headersFor)
                .map(headers -> base.mutate().defaultHeaders(h -> h.addAll(headers)).build())
                .orElse(base);
    }

    private HttpHeaders headersFor(CustomUserDetails principal) {
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-User-Id", principal.userId().toString());
        headers.add("X-User-Role", principal.role().name());
        return headers;
    }
}
