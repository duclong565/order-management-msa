package com.example.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import com.example.auth.common.UserRole;
import jakarta.validation.constraints.*;

//chuyển thành class
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateUserRequest {

    @NotBlank @Size(min = 3, max = 50)
    private String username;
    @NotBlank @Email
    private String email;
    @NotBlank @Size(min = 8, max = 255)
    private String password;
    @NotNull
    private UserRole role;
}
