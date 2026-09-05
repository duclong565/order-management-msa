package com.example.auth.security;

import com.example.auth.common.ErrorCode;
import com.example.auth.exception.ApplicationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class CurrentUserProvider {
    public Optional<CustomUserDetails> findPrincipal() {
        Authentication auth =
                SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated()
        || !(auth.getPrincipal() instanceof CustomUserDetails principal)) {
            return Optional.empty();
        }

        return Optional.of(principal);
    }

    public CustomUserDetails getPrincipal() {
        return findPrincipal()
                .orElseThrow(() -> new ApplicationException(ErrorCode.UNAUTHORIZED));
    }

    public UUID getUserId() {
        return getPrincipal().userId();
    }
}
