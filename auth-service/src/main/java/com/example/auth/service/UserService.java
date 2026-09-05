package com.example.auth.service;

import com.example.auth.dto.ChangePasswordRequest;
import com.example.auth.dto.CreateUserRequest;
import com.example.auth.dto.UserResponse;
import com.example.auth.entity.User;
import com.example.auth.common.ErrorCode;
import com.example.auth.exception.ApplicationException;
import com.example.auth.repository.UserRepository;
import com.example.auth.security.CurrentUserProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final CurrentUserProvider currentUserProvider;

    public UserResponse createUser(CreateUserRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ApplicationException(ErrorCode.EMAIL_EXISTED);
        }

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new ApplicationException(ErrorCode.USERNAME_EXISTED);
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEmail(request.getEmail());
        user.setRole(request.getRole());

        User savedUser = userRepository.save(user);

        return toResponse(savedUser);
    }

    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream().map(this::toResponse).toList();
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole(),
                user.getCreatedAt()
        );
    }

    @Transactional
    public void changePassword(ChangePasswordRequest request) {
        UUID userId = currentUserProvider.getUserId();

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApplicationException(ErrorCode.USER_NOT_FOUND));

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new ApplicationException(ErrorCode.WRONG_PASSWORD);
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
    }
}
