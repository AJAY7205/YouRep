package com.learning.ytrep.security.services;

import lombok.Data;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.learning.ytrep.model.User;

import lombok.AllArgsConstructor;

@Data
@AllArgsConstructor
public class UserDetailsImpl implements UserDetails{
    private Long id;
    private String username;
    private String email;

    @JsonIgnore
    private String password;

    private Collection<? extends GrantedAuthority> authorities;

    public static UserDetailsImpl build(User user){
        List<GrantedAuthority> authorities = user.getRoles().stream()
                                            .map(role -> new SimpleGrantedAuthority(role.getAppRole().name()))
                                            .collect(Collectors.toList());

                                return new UserDetailsImpl(user.getUserId(), user.getUsername(), user.getEmail(), user.getPassword(), authorities);
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

    // @Override
    public boolean isEnabled() {
        return true;
    }
}
