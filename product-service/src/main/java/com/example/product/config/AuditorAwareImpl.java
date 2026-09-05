package com.example.product.config;

import com.example.product.security.CurrentUserProvider;
import com.example.product.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.AuditorAware;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class AuditorAwareImpl implements AuditorAware<String> {
    private final CurrentUserProvider currentUserProvider;

    @Override
    public Optional<String> getCurrentAuditor() {
        return currentUserProvider.findPrincipal()
                .map(CustomUserDetails::getUsername);
    }
}
