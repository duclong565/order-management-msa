package com.example.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import com.example.auth.common.UserRole;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {

    private UUID id;
    private String username;
    private String email;
    private UserRole role;
    private Instant createdAt;
}
