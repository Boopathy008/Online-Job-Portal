package com.jobportal.security;

import com.jobportal.entity.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

public class CustomUserDetails implements UserDetails {

    private Long id;
    private String email;
    private String password;
    private Collection<? extends GrantedAuthority> authorities;
    private String name;

    public CustomUserDetails(Long id, String email, String password, Collection<? extends GrantedAuthority> authorities, String name) {
        this.id = id;
        this.email = email;
        this.password = password;
        this.authorities = authorities;
        this.name = name;
    }

    public static CustomUserDetails build(User user) {
        GrantedAuthority authority = new SimpleGrantedAuthority(user.getRole().name());

        return new CustomUserDetails(
                user.getId(),
                user.getEmail(),
                user.getPassword(),
                Collections.singletonList(authority),
                user.getName()
        );
    }

    public Long getId() {
        return id;
    }
    
    public String getName() {
        return name;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return true; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() { return true; }
}
