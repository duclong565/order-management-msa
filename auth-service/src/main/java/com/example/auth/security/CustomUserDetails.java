package com.example.auth.security;

import com.example.auth.entity.User;
import org.jspecify.annotations.NonNull;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public record CustomUserDetails(User user) implements UserDetails {

    //preAuthorize tự thêm prefix ROLE_ ở đằng trước vậy nên overide hàm này phải thêm cả ROLE_
    @Override
    @NonNull
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())); //Simple granted authority đơn giản là return lại chuỗi
    }

    @Override
    public String getPassword() {
        return user.getPassword();
    }

    @Override
    @NonNull
    public String getUsername() {
        return user.getUsername();
    }

}
