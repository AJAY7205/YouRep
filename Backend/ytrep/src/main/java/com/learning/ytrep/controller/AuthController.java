package com.learning.ytrep.controller;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.learning.ytrep.model.AppRole;
import com.learning.ytrep.model.Role;
import com.learning.ytrep.model.User;
import com.learning.ytrep.repository.RoleRepository;
import com.learning.ytrep.repository.UserRepository;
import com.learning.ytrep.security.jwt.JwtUtils;
import com.learning.ytrep.security.request.LoginRequest;
import com.learning.ytrep.security.request.SignupRequest;
import com.learning.ytrep.security.response.JwtResponse;
import com.learning.ytrep.security.response.MessageResponse;
import com.learning.ytrep.security.services.UserDetailsImpl;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder encoder;
    private final JwtUtils jwtUtils;

    public AuthController(
            AuthenticationManager authenticationManager,
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder encoder,
            JwtUtils jwtUtils) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.encoder = encoder;
        this.jwtUtils = jwtUtils;
    }
    @Operation(summary = "User Login")
    @PostMapping("/signin")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        
        // Check if account is locked
        User user = userRepository.findByUsername(loginRequest.getUsername())
                .orElse(null);
        
        if (user != null && user.isAccountLocked()) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Account is locked due to multiple failed login attempts"));
        }

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getUsername(),
                            loginRequest.getPassword()));

            SecurityContextHolder.getContext().setAuthentication(authentication);
            String jwt = jwtUtils.generateJwtToken(authentication);

            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
            List<String> roles = userDetails.getAuthorities().stream()
                    .map(item -> item.getAuthority())
                    .collect(Collectors.toList());

            // Reset failed attempts and update last login
            if (user != null) {
                user.setFailedLoginAttempts(0);
                user.setAccountLocked(false);
                user.setLastLogin(LocalDateTime.now());
                userRepository.save(user);
            }

            return ResponseEntity.ok(new JwtResponse(jwt,
                    userDetails.getId(),
                    userDetails.getUsername(),
                    userDetails.getEmail(),
                    roles));

        } catch (Exception e) {
            // Increment failed attempts
            if (user != null) {
                user.setFailedLoginAttempts(user.getFailedLoginAttempts() + 1);
                if (user.getFailedLoginAttempts() >= 5) {
                    user.setAccountLocked(true);
                }
                userRepository.save(user);
            }
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Invalid username or password"));
        }
    }
    @Operation(summary = "User Registration")
    @PostMapping("/signup")
    public ResponseEntity<?> registerUser(@Valid @RequestBody SignupRequest signUpRequest) {
        
        if (userRepository.existsByUsername(signUpRequest.getUsername())) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Error: Username is already taken!"));
        }

        if (userRepository.existsByEmail(signUpRequest.getEmail())) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Error: Email is already in use!"));
        }

        // Create new user
        User user = new User(signUpRequest.getUsername(),
                signUpRequest.getEmail(),
                encoder.encode(signUpRequest.getPassword()));

        Set<String> strRoles = signUpRequest.getRoles();
        Set<Role> roles = new HashSet<>();

        if (strRoles == null || strRoles.isEmpty()) {
            // Default role is USER
            Role userRole = roleRepository.findByAppRole(AppRole.USER)
                    .orElseGet(() -> {
                        Role newRole = new Role(AppRole.USER);
                        return roleRepository.save(newRole);
                    });
            roles.add(userRole);
        } else {
            strRoles.forEach(role -> {
                switch (role.toLowerCase()) {
                    case "admin":
                        // Admin signup is not allowed via API
                        throw new RuntimeException("Error: Admin role cannot be assigned via signup!");
                    case "guest":
                        Role guestRole = roleRepository.findByAppRole(AppRole.GUEST)
                                .orElseGet(() -> {
                                    Role newRole = new Role(AppRole.GUEST);
                                    return roleRepository.save(newRole);
                                });
                        roles.add(guestRole);
                        break;
                    default:
                        Role userRole = roleRepository.findByAppRole(AppRole.USER)
                                .orElseGet(() -> {
                                    Role newRole = new Role(AppRole.USER);
                                    return roleRepository.save(newRole);
                                });
                        roles.add(userRole);
                }
            });
        }

        user.setRoles(roles);
        userRepository.save(user);

        return ResponseEntity.ok(new MessageResponse("User registered successfully!"));
    }
}
