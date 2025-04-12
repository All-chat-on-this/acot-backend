package com.chat.allchatonthis.config.security.model;

import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Custom user details implementation for Spring Security
 */
@Data
public class LoginUser implements UserDetails {

    /**
     * User ID
     */
    private Long userId;

    /**
     * Username
     */
    private String username;

    /**
     * Password (not visible in responses)
     */
    private String password;

    /**
     * Nickname
     */
    private String nickname;

    /**
     * Login type (0: username/password, other values: social login types)
     */
    private Integer loginType;

    /**
     * Open ID for social login
     */
    private String openId;

    /**
     * User permissions/roles
     */
    private List<String> permissions;

    /**
     * Is account non-expired
     */
    private boolean accountNonExpired = true;

    /**
     * Is account non-locked
     */
    private boolean accountNonLocked = true;

    /**
     * Is credentials non-expired
     */
    private boolean credentialsNonExpired = true;

    /**
     * Is enabled
     */
    private boolean enabled = true;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if (permissions == null || permissions.isEmpty()) {
            return Collections.emptyList();
        }
        return permissions.stream()
                .map(SimpleGrantedAuthority::new)
                .toList();
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return accountNonExpired;
    }

    @Override
    public boolean isAccountNonLocked() {
        return accountNonLocked;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return credentialsNonExpired;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
} 