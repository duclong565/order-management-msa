package com.example.auth.controller;

import com.example.auth.common.BaseResponse;
import com.example.auth.dto.ChangePasswordRequest;
import com.example.auth.dto.CreateUserRequest;
import com.example.auth.dto.UserResponse;
import com.example.auth.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/users")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<BaseResponse<UserResponse>> createUser(@Valid @RequestBody CreateUserRequest createUserRequest) {
        UserResponse userResponse = userService.createUser(createUserRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(BaseResponse.success(userResponse));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')") // tự thêm prefix ROLE_ ở trước -> ROLE_ADMIN
    public ResponseEntity<BaseResponse<List<UserResponse>>> getAllUsers() {
        List<UserResponse> userResponseList = userService.getAllUsers();
        return ResponseEntity.status(HttpStatus.OK).body(BaseResponse.success(userResponseList));
    }

    // dùng bởi service khác (order-service...) qua UserClient - không phải endpoint cho end-user
    @GetMapping("/{id}")
    public ResponseEntity<BaseResponse<UserResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(BaseResponse.success(userService.getById(id)));
    }

    @PostMapping("/get-by-ids")
    public ResponseEntity<BaseResponse<List<UserResponse>>> getByIds(@RequestBody List<UUID> ids) {
        return ResponseEntity.ok(BaseResponse.success(userService.getByIds(ids)));
    }

    @PatchMapping("/me/password")
    public ResponseEntity<BaseResponse<Void>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request //@valid là để kích hoạt validation ở dto
            ) {
        userService.changePassword(request);
        return ResponseEntity.ok(BaseResponse.success(null, "Password changed successfully"));
    }
}
