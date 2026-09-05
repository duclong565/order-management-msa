package com.example.auth.entity;

import com.example.auth.common.UserRole;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "users")
@Getter
@Setter
public class User extends BaseEntity {
    @Column(nullable = false, unique = true)
    private String username;

    @Column(name = "full_name", length = 100)
    private String fullName;

    @Column(nullable = false ,unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private UserRole role;
}
