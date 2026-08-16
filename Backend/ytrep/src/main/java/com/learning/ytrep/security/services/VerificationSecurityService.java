package com.learning.ytrep.security.services;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import com.learning.ytrep.repository.UserRepository;

@Component("verificationSecurityService")
public class VerificationSecurityService {

    private final UserRepository userRepository;

    public VerificationSecurityService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public boolean isVerified(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        String username = authentication.getName();
        return userRepository.findByUsername(username)
                .map(user -> user.isEmailVerified())
                .orElse(false);
    }
}
