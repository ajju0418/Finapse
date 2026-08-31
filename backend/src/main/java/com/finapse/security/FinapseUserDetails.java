package com.finapse.security;

import com.finapse.entity.User;
import com.finapse.enums.Role;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Authenticated identity carried in the {@code SecurityContext}.
 *
 * <p>Two flavours exist: a full one loaded from the database during password
 * login, and a credential-free one rebuilt from access-token claims so that
 * ordinary requests do not need a user lookup.
 */
public class FinapseUserDetails implements UserDetails {

    private final UUID id;
    private final String email;
    private final String displayName;
    private final String passwordHash;
    private final Role role;
    private final boolean active;

    private FinapseUserDetails(UUID id, String email, String displayName,
                               String passwordHash, Role role, boolean active) {
        this.id = id;
        this.email = email;
        this.displayName = displayName;
        this.passwordHash = passwordHash;
        this.role = role;
        this.active = active;
    }

    public static FinapseUserDetails from(User user) {
        return new FinapseUserDetails(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getPasswordHash(),
                user.getRole(),
                user.isActive());
    }

    public static FinapseUserDetails fromClaims(UUID id, String email, String displayName, Role role) {
        return new FinapseUserDetails(id, email, displayName, null, role, true);
    }

    public UUID getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Role getRole() {
        return role;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(role.authority()));
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return active;
    }
}
