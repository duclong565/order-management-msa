package com.example.auth.config;

import com.example.auth.security.CurrentUserProvider;
import com.example.auth.security.CustomUserDetails;
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
