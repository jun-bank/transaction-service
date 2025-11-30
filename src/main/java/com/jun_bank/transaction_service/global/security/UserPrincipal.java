package com.jun_bank.transaction_service.global.security;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * 인증된 사용자 Principal
 * - Gateway에서 전달받은 사용자 정보를 담는 객체
 * - UserDetails 구현으로 Spring Security와 통합
 */
@Getter
@RequiredArgsConstructor
public class UserPrincipal implements UserDetails {

    private final String userId;
    private final String role;
    private final String email;

    public UserPrincipal(String userId, String role) {
        this(userId, role, null);
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role));
    }

    @Override
    public String getPassword() { return null; }

    @Override
    public String getUsername() { return userId; }

    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return true; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() { return true; }
}